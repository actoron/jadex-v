package jadex.mj.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.impl.IBootstrapping;
import jadex.mj.core.impl.IComponentCreator;
import jadex.mj.core.impl.IComponentTerminator;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.core.impl.SMjFeatureProvider;

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
	public IFuture<Void> terminate();
	
	//-------- static part for generic component creation --------
	
	public static final SMjFeatureProvider dummy = new SMjFeatureProvider();
	
	public static final Map<String, Set<IComponentListener>> listeners = new HashMap<String, Set<IComponentListener>>();
	public static final String COMPONENT_ADDED = "component_added";
	public static final String COMPONENT_REMOVED = "component_removed";
	public static final String COMPONENT_LASTREMOVED = "component_lastremoved";

	public static void addComponentListener(IComponentListener listener, String... types)
	{
		synchronized(IComponent.class)
		{	
			for(String type: types)
			{
				Set<IComponentListener> ls = listeners.get(type);
				if(ls==null)
				{
					ls = new HashSet<IComponentListener>();
					listeners.put(type, ls);
				}
				ls.add(listener);
			}
		}
	}
	
	public static void removeComponentListener(IComponentListener listener, String... types)
	{
		synchronized(IComponent.class)
		{
			for(String type: types)
			{
				Set<IComponentListener> ls = listeners.get(type);
				if(ls!=null)
				{
					ls.remove(listener);
					if(ls.isEmpty())
						listeners.remove(type);
				}
			}
		}
	}
	
	public static <T extends MjComponent> T	createComponent(Class<T> type, Supplier<T> creator)
	{
		List<MjFeatureProvider<Object>>	providers	= new ArrayList<>(SMjFeatureProvider.getProvidersForComponent(type).values());
		for(int i=providers.size()-1; i>=0; i--)
		{
			MjFeatureProvider<Object>	provider	= providers.get(i);
			if(provider instanceof IBootstrapping)
			{
				Supplier<T>	nextcreator	= creator;
				creator	= () -> ((IBootstrapping)provider).bootstrap(type, nextcreator);
			}
		}
		return creator.get();
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
		for(IComponentCreator creator: MjComponent.getCreators())
		{
			if(creator.filter(pojo))
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
		for(IComponentCreator creator: MjComponent.getCreators())
		{
			if(creator.filter(pojo))
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
		for(IComponentCreator creator: MjComponent.getCreators())
		{
			if(creator.filter(pojo))
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
		Future<Void> ret = new Future<Void>();
		
		IComponent comp = MjComponent.getComponent(cid);
		if(comp!=null)
		{
			MjComponent.removeComponent(cid);
			comp.getExternalAccess().scheduleStep(agent ->
			{
				boolean terminated = false;
				for(IComponentTerminator terminator: MjComponent.getTerminators())
				{
					if(terminator.filter((MjComponent)agent))
					{
						terminator.terminate(agent);
						terminated = true;
						ret.setResult(null);
						break;
					}
				}
				if(!terminated)
					ret.setException(new UnsupportedOperationException("No termination code for component: "+cid));
			});
		}
		else
		{
			ret.setException(new IllegalArgumentException("Component not found: "+cid));
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
