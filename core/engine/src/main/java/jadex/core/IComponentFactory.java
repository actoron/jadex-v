package jadex.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import jadex.common.NameValue;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SComponentFeatureProvider;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;

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
		
		if(pojo==null)
		{
			// Plain component for null pojo
			ret.setResult(Component.createComponent(Component.class, () -> new Component(pojo,cid,app)).getComponentHandle());
		}
		else
		{
			IComponentLifecycleManager	creator	= SComponentFeatureProvider.getCreator(pojo.getClass());
			if(creator!=null)
			{
				ret.setResult(creator.create(pojo, cid, app));
			}
			else
			{
				ret.setException(new RuntimeException("Could not create component: "+pojo));
			}
		}
		
		return ret;
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The function.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(IThrowingFunction<IComponent, T> pojo)
	{
		return run((Object)pojo);
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The callable.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(Callable<T> pojo)
	{
		return run((Object)pojo);
	}

	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The callable.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(IResultProvider pojo)
	{
		return run((Object)pojo);
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
		IComponentHandle handle = create(pojo).get();
		IComponent	comp	= ComponentManager.get().getComponent(handle.getId());
		
		// all run components that push notify on results will automatically get terminated after first result.
		subscribeToResults(comp)
			.next(r -> 
			{
//				System.out.println("received: "+r);	
				comp.terminate();
			});
//			.catchEx(e -> {})	// NOP on unsupported operation exception

		handle.waitForTermination().then(Void -> 
		{
			Map<String, Object> res = getResults(comp);
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
	public default Map<String, Object> getResults(IComponent component)
	{
		Map<String, Object> ret = new HashMap<>();
		boolean done = false;
		
		if(component.getPojo() instanceof IResultProvider)
		{
			IResultProvider rp = (IResultProvider)component.getPojo();
			ret = new HashMap<String, Object>(rp.getResultMap());
			done = true;
		}
		else if(component.getPojo()!=null)
		{
			IComponentLifecycleManager	creator	= SComponentFeatureProvider.getCreator(component.getPojo().getClass());
			if(creator!=null)
			{
				ret = creator.getResults(component);
				done = true;
			}
		}
		if(!done)
			throw new UnsupportedOperationException("Could not get results: "+component.getPojo());
		
		return ret;
	}
	
	public default ISubscriptionIntermediateFuture<NameValue> subscribeToResults(IComponent component)
	{
		ISubscriptionIntermediateFuture<NameValue>	ret	= null;
		boolean done = false;
		
		if(component.getPojo() instanceof IResultProvider)
		{
			IResultProvider rp = (IResultProvider)component.getPojo();
			ret = rp.subscribeToResults();
			done = true;
		}
		else if(component.getPojo()!=null)
		{
			IComponentLifecycleManager	creator	= SComponentFeatureProvider.getCreator(component.getPojo().getClass());
			if(creator!=null)
			{
				ret = creator.subscribeToResults(component);
				done = true;
			}
		}
		if(!done)
			throw new UnsupportedOperationException("Could not get results: "+component.getPojo());
		
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
	    	boolean[]	dowait	= new boolean[1];
		    //synchronized(ComponentManager.get().components) 
		    ComponentManager.get().runWithComponentsLock(() ->
		    {
		        if(ComponentManager.get().getNumberOfComponents() != 0) 
		        {
		        	dowait[0]	= true;
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
		        }
		    });
		    
		    if(dowait[0])
		    {
		    	try 
			    {
			    	wait.await();
			    } 
			    catch(InterruptedException e) 
			    {
			        e.printStackTrace();
			    }
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
