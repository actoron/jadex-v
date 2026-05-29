package jadex.core.impl;

import jadex.core.Application;
import jadex.core.ChangeEvent.Type;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Managing POJOs by creating/running/terminating corresponding components.
 */
public interface IComponentLifecycleManager
{
	/**
	 *  Check if a POJO can be handled.
	 *  
	 *  @return Priority: -1: cannot create, 0: fallback, 1: normal kernel, 2: extension of normal kernel, 3:...
	 */
	public int	isCreator(Class<?> pojoclazz);

	/**
	 *  Create a component for a POJO
	 */
	public IFuture<IComponentHandle>	create(Object pojo, ComponentIdentifier cid, Application app);

	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The pojo.
	 *  @param cid The component id or null for auto-generation.
	 *  @param app The application context.
	 *  @param async If true, the component is executed i.e. the result of the pojo is expected to be a future that needs unpacking.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T>	run(Object pojo, ComponentIdentifier cid, Application app, boolean async)
	{
		Future<T> ret = new Future<>();
		if(async)
		{
			ret.setException(new UnsupportedOperationException("Async execution not supported for pojo: "+pojo));
			return ret;
		}
		
		create(pojo, cid, app).then(handle -> 
		{
			handle.waitForTermination().then(Void -> 
			{
//				System.out.println("terminated");	
				handle.getResults().then(res->
				{
					if(res!=null && res.size()==1)
					{
						@SuppressWarnings("unchecked")
						T	result	= (T)res.values().iterator().next();
						ret.setResult(result);
					}
					else
					{
						ret.setException(new RuntimeException("no single result found: "+res));
					}
				})
				.catchEx(e -> ret.setException(e));
			});

			// all run components that push notify on results will automatically get terminated after first result.
			handle.subscribeToResults()
				.next(r -> 
				{
//					System.out.println("received: "+r);	
					// When initial value -> only terminate when != null
					if(r.type()!=Type.INITIAL || r.value()!=null)
					{
						handle.terminate();
					}
				});
//				.catchEx(e -> {})	// NOP on unsupported operation exception
		})
		.catchEx(e -> ret.setException(e));
		
		return ret;
	}
}