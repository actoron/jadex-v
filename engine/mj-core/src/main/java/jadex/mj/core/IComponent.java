package jadex.mj.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.impl.IComponentLifecycleManager;
import jadex.mj.core.impl.Component;
import jadex.mj.core.impl.SFeatureProvider;

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
	
	// todo: reduce this as metainfo
	/**
	 *  Get the model info.
	 *  @return The model info.
	 * /
	public IModelInfo getModel();*/
	
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
	
	//-------- static part for generic component creation --------
	
//	public static final SMjFeatureProvider dummy = new SMjFeatureProvider();
	
	public static final String COMPONENT_ADDED = "component_added";
	public static final String COMPONENT_REMOVED = "component_removed";
	public static final String COMPONENT_LASTREMOVED = "component_lastremoved";

	public static void addComponentListener(IComponentListener listener, String... types)
	{
		synchronized(Component.listeners)
		{	
			for(String type: types)
			{
				Set<IComponentListener> ls = Component.listeners.get(type);
				if(ls==null)
				{
					ls = new HashSet<IComponentListener>();
					Component.listeners.put(type, ls);
				}
				ls.add(listener);
			}
		}
	}
	
	public static void removeComponentListener(IComponentListener listener, String... types)
	{
		synchronized(Component.listeners)
		{
			for(String type: types)
			{
				Set<IComponentListener> ls = Component.listeners.get(type);
				if(ls!=null)
				{
					ls.remove(listener);
					if(ls.isEmpty())
						Component.listeners.remove(type);
				}
			}
		}
	}
	
	
	
	/*public static Class<? extends MjComponent> findComponentType(Object pojo)
	{
		Class<? extends MjComponent> ret = null;
		for(IComponentCreator finder: creators)
		{
			if(finder.filter(finder))
			{
				ret = finder.getType();
				break;
			}
		}
		return ret;
	}*/
	
	public static void create(Runnable pojo)
	{
		create(pojo, null);
	}
	
	public static void create(Runnable pojo, ComponentIdentifier cid)
	{
		boolean created = false;
		
		
		
		for(IComponentLifecycleManager creator: SFeatureProvider.getLifecycleProviders())
		{
			if(creator.isCreator(pojo))
			{
				creator.create(pojo, cid);
				created = true;
				break;
			}
		}
		if(!created)
			throw new RuntimeException("Could not create component: "+pojo);
	}
	
	// todo: support return IFuture<T> ?!
	public static <T> void create(IThrowingFunction<IComponent, T> pojo)
	{
		create(pojo, null);
	}
	
	// todo: support return IFuture<T> ?!
	public static <T> void create(IThrowingFunction<IComponent, T> pojo, ComponentIdentifier cid)
	{
		boolean created = false;
		
		for(IComponentLifecycleManager creator: SFeatureProvider.getLifecycleProviders())
		{
			if(creator.isCreator(pojo))
			{
				creator.create(pojo, cid);
				created = true;
				break;
			}
		}
		if(!created)
			throw new RuntimeException("Could not create component: "+pojo);
	}
	
	public static void create(Object pojo)
	{
		create(pojo, null);
	}
	
	public static void create(Object pojo, ComponentIdentifier cid)
	{
		boolean created = false;
		for(IComponentLifecycleManager creator: SFeatureProvider.getLifecycleProviders())
		{
			if(creator.isCreator(pojo))
			{
				creator.create(pojo, cid);
				created = true;
				break;
			}
		}
		if(!created)
			throw new RuntimeException("Could not create component: "+pojo);
	}
	
	public static IFuture<Void> terminate(ComponentIdentifier cid)
	{
		IFuture<Void> ret;
		
		try
		{
			IComponent comp = Component.getComponent(cid);
			IExternalAccess	exta	= comp.getExternalAccess();
			Component.removeComponent(cid);
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
					System.out.println("removed last: "+cid);
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
	
}
