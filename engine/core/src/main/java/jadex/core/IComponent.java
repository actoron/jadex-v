package jadex.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import jadex.core.impl.ComponentManager;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SFeatureProvider;
import jadex.future.Future;
import jadex.future.IFuture;

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
	 *  Get the feature instance for the given type.
	 *  Instantiates lazy features if needed.
	 */
	public <T> T getFeature(Class<T> type);
	
	/**
	 *  Get the external access.
	 *  @return The external access.
	 */
	public IExternalAccess getExternalAccess();
	
	/**
	 *  Get the external access.
	 *  @param The id of the component.
	 *  @return The external access.
	 */
	public IExternalAccess getExternalAccess(ComponentIdentifier cid);
	
	/**
	 *  Terminate the component.
	 */
	public void terminate();
	// todo: terminate(Exception e) ?!
	
	//-------- static part for generic component creation --------
	
//	public static final SMjFeatureProvider dummy = new SMjFeatureProvider();
	
	public static final String COMPONENT_ADDED = "component_added";
	public static final String COMPONENT_REMOVED = "component_removed";
	public static final String COMPONENT_LASTREMOVED = "component_lastremoved";

	public static void addComponentListener(IComponentListener listener, String... types)
	{
		synchronized(ComponentManager.get().listeners)
		{	
			for(String type: types)
			{
				Set<IComponentListener> ls = ComponentManager.get().listeners.get(type);
				if(ls==null)
				{
					ls = new HashSet<IComponentListener>();
					ComponentManager.get().listeners.put(type, ls);
				}
				ls.add(listener);
			}
		}
	}
	
	public static void removeComponentListener(IComponentListener listener, String... types)
	{
		synchronized(ComponentManager.get().listeners)
		{
			for(String type: types)
			{
				Set<IComponentListener> ls = ComponentManager.get().listeners.get(type);
				if(ls!=null)
				{
					ls.remove(listener);
					if(ls.isEmpty())
						ComponentManager.get().listeners.remove(type);
				}
			}
		}
	}
	
	// todo: remove
	public static IFuture<IExternalAccess> create(Runnable pojo)
	{
		return create(pojo, null);
	}
	
	// todo: remove
	public static IFuture<IExternalAccess> create(Runnable pojo, ComponentIdentifier cid)
	{
		return create((Object)pojo, cid);
	}
	
	// todo: remove
	public static <T> IFuture<IExternalAccess> create(IThrowingFunction<IComponent, T> pojo)
	{
		return create(pojo, null);
	}
	
	// todo: remove
	public static <T> IFuture<IExternalAccess> create(IThrowingFunction<IComponent, T> pojo, ComponentIdentifier cid)
	{
		return create((Object)pojo, cid);
	}
	
	public static IFuture<IExternalAccess> create(Object pojo)
	{
		return create(pojo, null);
	}
	
	public static IFuture<IExternalAccess> create(Object pojo, ComponentIdentifier cid)
	{
		Future<IExternalAccess> ret = new Future<>();
		
		boolean created = false;
		for(IComponentLifecycleManager creator: SFeatureProvider.getLifecycleProviders())
		{
			if(creator.isCreator(pojo))
			{
				ret.setResult(creator.create(pojo, cid));
				created = true;
				break;
			}
		}
		if(!created)
			ret.setException(new RuntimeException("Could not create component: "+pojo));
		
		return ret;
	}
	
	// todo: return pojo as result (has results)
	public static IFuture<Void> terminate(ComponentIdentifier cid)
	{
		IFuture<Void> ret;
		
		//System.out.println("terminate: "+cid);
		
		try
		{
			IComponent comp = ComponentManager.get().getComponent(cid);
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
		catch(Exception e)
		{
			ret	= new Future<>(e);
		}
		
		return ret;
	}
	
	public static void waitForLastComponentTerminated()
	{
		try
		{
			Semaphore sem = new Semaphore(0);
			IComponent.addComponentListener(new IComponentListener() 
			{
				@Override
				public void lastComponentRemoved(ComponentIdentifier cid) 
				{
					//System.out.println("removed last: "+cid);
					sem.release();
				}
			}, IComponent.COMPONENT_LASTREMOVED);
			sem.acquire();
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
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
