package jadex.core;

import java.lang.System.Logger;

import jadex.core.impl.ValueProvider;

/**
 *  Interface for a component.
 */
public interface IComponent 
{
	/**
	 *  Get the id.
	 *  @return The id.
	 */
	public ComponentIdentifier getId();
	
	/**
	 *  Get the application.
	 */
	public Application getApplication();
	
	/**
	 *  Get the app id.
	 *  return The app id.
	 */
	public String getAppId();
	
	/**
	 *  Get the feature instance for the given type.
	 *  Instantiates lazy features if needed.
	 */
	public <T extends IComponentFeature> T getFeature(Class<T> type);
	
	/**
	 *  Get the component handle.
	 *  @return The handle.
	 */
	public IComponentHandle getComponentHandle();
	
	/**
	 *  Check if the component is terminated.
	 */
	public boolean isTerminated();

	/**
	 *  Terminate the component.
	 */
	public  void	terminate();
	
	/**
	 *  Get the last exception, if any.
	 */
	public Exception getException();
	
	/**
	 *  Get the pojo.
	 *  @return The pojo.
	 */
	public Object getPojo();

	/**
	 *  Returns the appropriate logging access for the component.
	 *
	 *  @return The component logger.
	 */
	public Logger getLogger();
	
	/**
	 *  Get the value provider (for fetcher and parameter guesser).
	 *  @return The value provider.
	 */
	public ValueProvider getValueProvider();
	
	/**
	 *  Wait for termination.
	 *  @return True on termination; false on component not found.
	 * /
	public default IFuture<Boolean> waitForTermination()
	{
		return IComponent.waitForTermination(getId());
	}*/
	
	//-------- static part for generic component creation --------
	
//	public static final SMjFeatureProvider dummy = new SMjFeatureProvider();
	
	
	
	// todo: remove
	/*public static IFuture<IExternalAccess> create(Runnable pojo)
	{
		return create(pojo, null);
	}*/
	
	// todo: remove
	/*public static IFuture<IExternalAccess> create(Runnable pojo, ComponentIdentifier cid)
	{
		return create((Object)pojo, cid);
	}*/
	
	// todo: remove
	/*public static <T> IFuture<IExternalAccess> create(IThrowingFunction<IComponent, T> pojo)
	{
		return create(pojo, null);
	}*/
	
	// todo: remove
	/*public static <T> IFuture<IExternalAccess> create(IThrowingFunction<IComponent, T> pojo, ComponentIdentifier cid)
	{
		return create((Object)pojo, cid);
	}*/
	
	/*public static IFuture<IExternalAccess> create(Object pojo)
	{
		return create(pojo, null);
	}*/
	
	/*public static IFuture<IExternalAccess> create(Object pojo, Application app)
	{
		return create(pojo, null, app);
	}*/
	
	/*public static IFuture<IExternalAccess> create(Object pojo, ComponentIdentifier cid, Application app)
	{		
		Future<IExternalAccess> ret = new Future<>();
		
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
	}*/
	
	// todo: return pojo as result (has results)
	/*public static IFuture<Void> terminate(ComponentIdentifier cid)
	{
		IFuture<Void> ret;
		
		//System.out.println("terminate: "+cid+" comps: "+ComponentManager.get().getNumberOfComponents());
		
		try
		{
			IComponent comp = ComponentManager.get().getComponent(cid);
			if(comp!=null)
			{
				IExternalAccess	exta = comp.getExternalAccess();
				//ComponentManager.get().removeComponent(cid); // done in Component
				if(exta.isExecutable())
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
					comp.terminate();
					ret	= IFuture.DONE;
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
	}*/
	
	/*public static void waitForLastComponentTerminated() 
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
	}*/
	
	/**
	 *  Wait for termination.
	 *  @param cid The component id;
	 *  @return True on termination; false on component not found.
	 * /
	public static IFuture<Boolean> waitForTermination(ComponentIdentifier cid)
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
	}*/
	
	/*public static <T> IFuture<T> run(IThrowingFunction<IComponent, T> body)
	{
		LambdaPojo<T> pojo = new LambdaPojo<T>(body);
		return run(pojo);
	}*/
	
	/*public static <T> IFuture<T> run(Callable<T> body)
	{
		LambdaPojo<T> pojo = new LambdaPojo<T>(body);
		return run(pojo);
	}*/
	
	/*public static <T> IFuture<T> run(Object pojo)
	{
		Future<T> ret = new Future<>();
		IExternalAccess comp = IComponentManager.get().create(pojo).get();
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
			Map<String, Object> res = IComponent.getResults(pojo);
			if(res.size()==1)
				ret.setResult((T)res.values().iterator().next());
			else
				ret.setException(new RuntimeException("no result found: "+res));
		});
		
		return ret;
	}*/
	
	/*public static Map<String, Object> getResults(Object pojo)
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
	}*/
	
	/**
	 *  Get the external access.
	 *  @param cid The component id.
	 *  @return The external access.
	 * /
	public static IExternalAccess getExternalComponentAccess(ComponentIdentifier cid)
	{
		return ComponentManager.get().getComponent(cid).getExternalAccess();
	}*/
	
}
