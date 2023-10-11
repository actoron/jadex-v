package jadex.mj.core;

import java.util.ArrayList;
import java.util.List;
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
	public void terminate();
	
	//-------- static part for generic component creation --------
	
	public static final List<IComponentCreator> creators = new ArrayList<IComponentCreator>();
	public static final List<IComponentTerminator> terminators = new ArrayList<IComponentTerminator>();
	
	public static final SMjFeatureProvider dummy = new SMjFeatureProvider();
	
	public static void addComponentCreator(IComponentCreator creator)
	{
		creators.add(creator);
	}
	
	public static void addComponentTerminator(IComponentTerminator terminator)
	{
		terminators.add(terminator);
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
		for(IComponentCreator creator: creators)
		{
			if(creator.filter(pojo))
			{
				creator.create(pojo, null);
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
		for(IComponentCreator creator: creators)
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
			comp.getExternalAccess().scheduleStep(agent ->
			{
				boolean terminated = false;
				for(IComponentTerminator terminator: terminators)
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
					ret.setException(new RuntimeException("Could not terminate component: "+cid));
			});
		}
		else
		{
			ret.setException(new RuntimeException("Component not found: "+cid));
		}
		
		return ret;
	}
	
}
