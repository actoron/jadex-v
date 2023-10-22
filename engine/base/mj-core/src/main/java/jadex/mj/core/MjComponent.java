package jadex.mj.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import jadex.common.IParameterGuesser;
import jadex.common.IValueFetcher;
import jadex.common.SReflect;
import jadex.mj.core.impl.IBootstrapping;
import jadex.mj.core.impl.IComponentLifecycleManager;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.core.impl.SMjFeatureProvider;
import jadex.mj.core.modelinfo.IModelInfo;
import jadex.mj.core.modelinfo.ModelInfo;

/**
 *  Base class for Jadex components, which provides access to component features.
 */
public class MjComponent implements IComponent
{
	protected static Map<ComponentIdentifier, IComponent> components = new LinkedHashMap<ComponentIdentifier, IComponent>();
	
	/** The providers for this component type, stored by the feature type they provide.
	 *  Is also used at runtime to instantiate lazy features.*/
	protected Map<Class<Object>, MjFeatureProvider<Object>>	providers;
	
	/** The feature instances of this component, stored by the feature type. */
	protected Map<Class<Object>, Object>	features;
	
	/** The fetcher. */
	protected IValueFetcher fetcher;
	
	/** The model. */
	protected IModelInfo modelinfo;
	
	/** The id. */
	protected ComponentIdentifier id;
	
	/** The external access. */
	protected IExternalAccess access;
	
	/** The external access supplier. */
	protected static Function<MjComponent, IExternalAccess> accessfactory;
	
	/**
	 *  Create a new component and instantiate all features (except lazy features).
	 */
	public MjComponent(IModelInfo modelinfo)
	{
		this(modelinfo, null);
	}
	
	/**
	 *  Create a new component and instantiate all features (except lazy features).
	 */
	public MjComponent(IModelInfo modelinfo, ComponentIdentifier id)
	{
		this.modelinfo = modelinfo;
		this.id = id==null? new ComponentIdentifier(): id;
		//System.out.println(this.id.getLocalName());
		MjComponent.addComponent(this); // is this good here?! 
		
		providers	= SMjFeatureProvider.getProvidersForComponent(getClass());
		
		// Instantiate all features (except lazy ones).
		providers.values().forEach(provider ->
		{
			if(!provider.isLazyFeature())
			{
				Object	feature	= provider.createFeatureInstance(this);
				putFeature(provider.getFeatureType(), feature);
				//features.put(provider.getFeatureType(), feature);
			}
		});
	}
	
	public static void addComponent(IComponent comp)
	{
		//System.out.println("added: "+comp.getId());
		synchronized(components)
		{
			if(components.containsKey(comp.getId()))
				throw new IllegalArgumentException("Component with same CID already exists: "+comp.getId());
			components.put(comp.getId(), comp);
		}
		notifyEventListener(COMPONENT_ADDED, comp.getId());
	}
	
	public static void removeComponent(ComponentIdentifier cid)
	{
		boolean last;
		synchronized(components)
		{
			components.remove(cid);
			last	= components.isEmpty();
		}
		notifyEventListener(COMPONENT_REMOVED, cid);
		if(last)
			notifyEventListener(COMPONENT_LASTREMOVED, cid);
//		System.out.println("size: "+components.size()+" "+cid);
	}
	
	/**
	 *  Get a running component.
	 *  @throws IllegalArgumentException when the component does not exist.
	 */
	public static IComponent getComponent(ComponentIdentifier cid)
	{
		synchronized(components)
		{
			return components.get(cid);
		}
	}
	
	public static void notifyEventListener(String type, ComponentIdentifier cid)
	{
		Set<IComponentListener> mylisteners = null;
		
		synchronized(IComponent.class)
		{
			Set<IComponentListener> ls = listeners.get(type);
			if(ls!=null)
				mylisteners = new HashSet<IComponentListener>(ls);
		}
		
		if(mylisteners!=null)
		{
			if(COMPONENT_ADDED.equals(type))
				mylisteners.stream().forEach(lis -> lis.componentAdded(cid));
			else if(COMPONENT_REMOVED.equals(type))
				mylisteners.stream().forEach(lis -> lis.componentRemoved(cid));
			else if(COMPONENT_LASTREMOVED.equals(type))
				mylisteners.stream().forEach(lis -> lis.lastComponentRemoved(cid));
		}
	}
	
	/**
	 *  Get the id.
	 *  @return The id.
	 */
	public ComponentIdentifier getId() 
	{
		return id;
	}

	/**
	 *  Get the internal set of currently instantiated features.
	 *  Does not include lazy, which have not yet been accessed.  
	 */
	public Collection<Object>	getFeatures()
	{
		return features!=null ? (Collection<Object>)features.values() : Collections.emptySet();
	}
	
	// TODO: needed?
//	/**
//	 *  Get the feature instance for the given type.
//	 *  Instantiates lazy features if needed.
//	 */
//	public <T> T getExistingFeature(Class<T> type)
//	{
//		//return getFeatures().stream().findFirst(feature -> feature instanceof IMjLifecycle);
//		return getFeatures().stream()
//	        .filter(feature -> type.isInstance(feature))
//	        .map(type::cast)  
//	        .findFirst()
//	        .orElse(null); 
//	}
	
	/**
	 *  Get the feature instance for the given type.
	 *  Instantiates lazy features if needed.
	 */
	public <T> T getFeature(Class<T> type)
	{
		if(features!=null && features.containsKey(type))
		{
			@SuppressWarnings("unchecked")
			T	ret	= (T)features.get(type);
			return ret;
		}
		else if(providers.containsKey(type))
		{
			try
			{
				MjFeatureProvider<?>	provider	= providers.get(type);
				assert provider.isLazyFeature();
				@SuppressWarnings("unchecked")
				T	ret	= (T)provider.createFeatureInstance(this);
				@SuppressWarnings("unchecked")
				Class<Object> otype	= (Class<Object>)type;
				putFeature(otype, ret);
				//features.put(otype, ret);
				return ret;
			}
			catch(Throwable t)
			{
				throw SMjFeatureProvider.throwUnchecked(t);
			}
		}
		else
		{
			throw new RuntimeException("No such feature: "+type);
		}
	}
	
	/**
	 *  Terminate the component.
	 */
	public void	terminate()
	{
		Map<Class<Object>, MjFeatureProvider<Object>> provs = SMjFeatureProvider.getProvidersForComponent((Class<? extends MjComponent>)getClass());
		Optional<IComponentLifecycleManager> opt = provs.values().stream().filter(provider -> provider instanceof IComponentLifecycleManager).map(provider -> (IComponentLifecycleManager)provider).findFirst();
		if(opt.isPresent())
		{
			IComponentLifecycleManager lm =  opt.get();
			lm.terminate(this);
//			return;
		}
//		throw new UnsupportedOperationException("No termination code for component: "+getId());
	}
	
	protected void putFeature(Class<Object> type, Object feature)
	{
//		System.out.println("putFeature: "+type+" "+feature);
		if(features==null)
		{
			features	= new LinkedHashMap<>(providers.size(), 1);
		}
		features.put(type, feature);
	}
	
	/**
	 *  Get the fetcher.
	 *  @return The fetcher.
	 */
	public IValueFetcher getFetcher()
	{
		if(fetcher==null)
		{
			// Return a fetcher that tries features in reverse order first.
			return new IValueFetcher()
			{
				public Object fetchValue(String name)
				{
					Object	ret	= null;
					boolean	found	= false;
					
					Object[] lfeatures = getFeatures().toArray();
					for(int i=lfeatures.length-1; !found && i>=0; i--)
					{
						//if(lfeatures[i] instanceof IValueFetcherProvider)
						if(lfeatures[i] instanceof IValueFetcher)
						{
							//IValueFetcher	vf	= ((IValueFetcherProvider)lfeatures[i]).getValueFetcher();
							//if(vf!=null)
							//{
								try
								{
									// Todo: better (faster) way than throwing exceptions?
									ret = ((IValueFetcher)lfeatures[i]).fetchValue(name);
									//ret	= vf.fetchValue(name);
									found	= true;
								}
								catch(Exception e)
								{
								}
							//}
						}
					}
					
					/*if(!found && "$component".equals(name))
					{
						ret	= getInternalAccess();
						found	= true;
					}
					else if(!found && "$config".equals(name))
					{
						ret	= getConfiguration();
						found	= true;
					}*/
					
					if(!found)
						throw new RuntimeException("Value not found: "+name);
//					else
//						System.out.println("fetcher: "+name+" "+ret);
					
					return ret;
				}
			};
		}
		
		return fetcher;
	}
	
	public IParameterGuesser getParameterGuesser()
	{
		// Return a fetcher that tries features first.
		// Todo: better (faster) way than throwing exceptions?
		return new IParameterGuesser()
		{
//			IParameterGuesser parent;
//			
//			public void setParent(IParameterGuesser parent)
//			{
//				this.parent = parent;
//			}
//			
//			public IParameterGuesser getParent()
//			{
//				return parent;
//			}
			
			public Object guessParameter(Class<?> type, boolean exact)
			{
				Object	ret	= null;
				boolean	found = false;
				
				Object[] lfeatures = getFeatures().toArray();
				for(int i=lfeatures.length-1; !found && i>=0; i--)
				{
					try
					{
						if(lfeatures[i] instanceof IParameterGuesserProvider)
						{
							IParameterGuesser pg = ((IParameterGuesserProvider)lfeatures[i]).getParameterGuesser();
							if(pg!=null)
							{
								ret	= pg.guessParameter(type, exact);
								found	= true;
							}
						}
					}
					catch(Exception e)
					{
					}
				}
				
				if(!found && ((exact && IComponent.class.equals(type))
					|| (!exact && SReflect.isSupertype(IComponent.class, type))))
				{
					ret	= MjComponent.this;
					found	= true;
				}
				
				/*if(!found && ((exact && IInternalAccess.class.equals(type))
					|| (!exact && SReflect.isSupertype(type, IInternalAccess.class))))
				{
					ret	= getInternalAccess();
					found	= true;
				}
				else if(!found && ((exact && IExternalAccess.class.equals(type))
					|| (!exact && SReflect.isSupertype(type, IExternalAccess.class))))
				{
					ret	= getExternalAccess();
					found	= true;
				}*/
				
				if(!found)
					throw new RuntimeException("Value not found: "+type);
				
				return ret;
			}
			
		};
	}
	
	/**
	 *  Get the model info.
	 *  @return The model info.
	 */
	public ModelInfo getModel()
	{
		return (ModelInfo)modelinfo;
	}
	
	/**
	 *  Get the external access.
	 *  @return The external access.
	 */
	public IExternalAccess getExternalAccess()
	{
		if(access==null)
		{
			if(accessfactory!=null)
			{
				access = accessfactory.apply(this);
			}
			else
			{
				access = new IExternalAccess() 
				{
					@Override
					public ComponentIdentifier getId() 
					{
						return MjComponent.this.getId();
					}
				};
			}
		}
		return access;
	}
	
	/**
	 *  Get the external access.
	 *  @param cid The component id.
	 *  @return The external access.
	 */
	public IExternalAccess getExternalAccess(ComponentIdentifier cid)
	{
		return MjComponent.getComponent(cid).getExternalAccess();
	}
	
	/**
	 *  Set the external access factory.
	 *  @param factory The factory.
	 */
	public static void setExternalAccessFactory(Function<MjComponent, IExternalAccess> factory)
	{
		accessfactory = factory;
	}
	
	public ClassLoader getClassLoader()
	{
		return this.getClass().getClassLoader();
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
}
