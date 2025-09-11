package jadex.core.impl;

import java.util.Map;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.ResultEvent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;

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
	 *  Fetch the result(s) of the POJO.
	 */
	public default Map<String, Object> getResults(IComponent component)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Listen to results of the pojo.
	 *  @throws UnsupportedOperationException when subscription is not supported
	 */
	public default ISubscriptionIntermediateFuture<ResultEvent> subscribeToResults(IComponent component)
	{
		return new SubscriptionIntermediateFuture<>(new UnsupportedOperationException());
	}

	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The pojo.
	 *  @param cid The component id or null for auto-generationm.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T>	run(Object pojo, ComponentIdentifier cid, Application app)
	{
		Future<T> ret = new Future<>();
		create(pojo, cid, app).then(handle -> 
		{
			handle.waitForTermination().then(Void -> 
			{
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
					handle.terminate();
				});
//				.catchEx(e -> {})	// NOP on unsupported operation exception
		})
		.catchEx(e -> ret.setException(e));
		
		return ret;
	}
}