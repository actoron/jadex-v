package jadex.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SComponentFeatureProvider;
import jadex.future.Future;
import jadex.future.IFuture;

public interface IComponentFactory
{
	/**
	 *  Create a component based on a pojo.
	 *  @param pojo The pojo.
	 *  @return The external access of the running component.
	 */
	public default IFuture<IComponentHandle> create(Object pojo)
	{
		return create(pojo, null);
	}
	
	/**
	 *  Create a component based on a pojo.
	 *  @param pojo The pojo.
	 *  @param cid The component id.
	 *  @return The external access of the running component.
	 */
	public default IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid)
	{		
		return create(pojo, null, null);
	}
	
	/**
	 *  Create a component based on a pojo.
	 *  @param pojo The pojo.
	 *  @param cid The component id.
	 *  @param app The application context.
	 *  @return The external access of the running component.
	 */
	public default IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{		
		Future<IComponentHandle> ret = new Future<>();
		
		boolean created = false;
		for(IComponentLifecycleManager creator: SComponentFeatureProvider.getLifecycleProviders())
		{
			if(creator.isCreator(pojo))
			{
				ret.setResult(creator.create(pojo, cid, app));
				created = true;
				break;
			}
		}
		if(!created)
			ret.setException(new RuntimeException("Could not create component: "+pojo));
		
		return ret;
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param body The function.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(IThrowingFunction<IComponent, T> body)
	{
		LambdaPojo<T> pojo = new LambdaPojo<T>(body);
		return run(pojo);
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param body The callable.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(Callable<T> body)
	{
		LambdaPojo<T> pojo = new LambdaPojo<T>(body);
		return run(pojo);
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The pojo.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(Object pojo)
	{
		Future<T> ret = new Future<>();
		IComponentHandle comp = create(pojo).get();
		// all pojos of type IResultProvider will be terminate component after result is received
		if(pojo instanceof IResultProvider)
		{
			((IResultProvider)pojo).subscribeToResults().next(r -> 
			{
				//System.out.println("received: "+r);	
				comp.terminate();
			});
		}
		comp.waitForTermination().then(Void -> 
		{
			Map<String, Object> res = getResults(pojo);
			if(res.size()==1)
				ret.setResult((T)res.values().iterator().next());
			else
				ret.setException(new RuntimeException("no result found: "+res));
		});
		
		return ret;
	}
	
	/**
	 *  Terminate a component with given id.
	 *  @param cid The component id.
	 */
	// todo: return pojo as result (has results)
	public default IFuture<Void> terminate(ComponentIdentifier cid)
	{
		IFuture<Void> ret;
		
		//System.out.println("terminate: "+cid+" comps: "+ComponentManager.get().getNumberOfComponents());
		
		try
		{
			IComponent comp = ComponentManager.get().getComponent(cid);
			if(comp!=null)
			{
				IComponentHandle	exta = comp.getComponentHandle();
				//ComponentManager.get().removeComponent(cid); // done in Component
				if(Component.isExecutable())
				{
					ret	= exta.scheduleStep(icomp ->
					{
						icomp.terminate();
						return (Void)null;
					});
				}
				else
				{
					// Hack!!! Concurrency issue?
					ret	= comp.terminate();
				}
			}
			else
			{
				ret	= new Future<>(new ComponentNotFoundException(cid));
			}
		}
		catch(Exception e)
		{
			ret	= new Future<>(e);
		}
		
		return ret;
	}
	
	/**
	 *  Extract the results from a pojo.
	 *  @return The result map.
	 */
	public default Map<String, Object> getResults(Object pojo)
	{
		Map<String, Object> ret = new HashMap<>();
		boolean done = false;
		
		if(pojo instanceof IResultProvider)
		{
			IResultProvider rp = (IResultProvider)pojo;
			ret = new HashMap<String, Object>(rp.getResultMap());
			done = true;
		}
		else
		{
			for(IComponentLifecycleManager creator: SComponentFeatureProvider.getLifecycleProviders())
			{
				if(creator.isCreator(pojo))
				{
					ret = creator.getResults(pojo);
					done = true;
					break;
				}
			}
		}
		if(!done)
			throw new RuntimeException("Could not get results: "+pojo);
		
		return ret;
	}
	
	/**
	 *  Wait for the last component being terminated.
	 *  This call keeps the calling thread waiting till termination.
	 */
	public default void waitForLastComponentTerminated()
	{
		// Use reentrant lock/condition instead of synchronized/wait/notify to avoid pinning when using virtual threads.
		ReentrantLock lock	= new ReentrantLock();
		Condition	wait	= lock.newCondition();

	    try 
	    { 
	    	lock.lock();
		    //synchronized(ComponentManager.get().components) 
		    ComponentManager.get().runWithComponentsLock(() ->
		    {
		        if(ComponentManager.get().getNumberOfComponents() == 0) 
		        {
		            return;
		        }
		        IComponentManager.get().addComponentListener(new IComponentListener() 
		        {
		            @Override
		            public void lastComponentRemoved(ComponentIdentifier cid) 
		            {
		        	    try 
		        	    { 
		        	    	lock.lock();
		        	    	IComponentManager.get().removeComponentListener(this, IComponentManager.COMPONENT_LASTREMOVED);
		                    wait.signal();
		                }
		        	    finally
		        	    {
		        			lock.unlock();
		        		}
		            }
		        }, IComponentManager.COMPONENT_LASTREMOVED);
		    });
		    
	    	try 
		    {
		    	wait.await();
		    } 
		    catch(InterruptedException e) 
		    {
		        e.printStackTrace();
		    }
	    }
	    finally
	    {
			lock.unlock();
		}
	}
	
	/**
	 *  Wait for termination of a component.
	 *  @param cid The component id;
	 *  @return True on termination; false on component not found.
	 */
	public default IFuture<Boolean> waitForTermination(ComponentIdentifier cid)
	{
		Future<Boolean> ret = new Future<>();
		boolean[] found = new boolean[1];
		//synchronized(ComponentManager.get().components)
		ComponentManager.get().runWithComponentsLock(() ->
		{
			if(ComponentManager.get().getComponent(cid)!=null)
			{
				found[0] = true;
				IComponentManager.get().addComponentListener(new IComponentListener() 
				{
					@Override
					public void componentRemoved(ComponentIdentifier ccid) 
					{
						if(cid.equals(ccid))
						{
							IComponentManager.get().removeComponentListener(this, IComponentManager.COMPONENT_REMOVED);
							ret.setResult(true);
						}
					}
				}, IComponentManager.COMPONENT_REMOVED);
			}
		});
		if(!found[0])
			ret.setResult(false);
		return ret;
	}

}
