package jadex.mj.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import jadex.common.IParameterGuesser;
import jadex.common.IValueFetcher;
import jadex.common.SReflect;
import jadex.future.IFuture;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.core.impl.SMjFeatureProvider;
import jadex.mj.core.modelinfo.IModelInfo;
import jadex.mj.core.modelinfo.ModelInfo;

/**
 *  Base class for Jadex components, which provides access to component features.
 */
public class MjComponent implements IComponent
{
	protected static Map<ComponentIdentifier, IComponent> components = Collections.synchronizedMap(new HashMap<ComponentIdentifier, IComponent>());
	
	/** The providers for this component type, stored by the feature type they provide.
	 *  Is also used at runtime to instantiate lazy features.*/
	protected Map<Class<Object>, MjFeatureProvider<Object>>	providers;
	
	/** The feature instances of this component, stored by the feature type. */
	protected Map<Class<Object>, Object>	features	= new LinkedHashMap<>();
	
	/** The fetcher. */
	protected IValueFetcher fetcher;
	
	/** The model. */
	protected IModelInfo modelinfo;
	
	/** The id. */
	protected ComponentIdentifier id;
	
	/** The external access. */
	protected IExternalAccess access;
	
	/** The external access supplier. */
	protected static Function<Object, IExternalAccess> accessfactory;
	
	/**
	 *  Create a new component and instantiate all features (except lazy features).
	 */
	protected MjComponent(IModelInfo modelinfo)
	{
		this(modelinfo, null);
	}
	
	/**
	 *  Create a new component and instantiate all features (except lazy features).
	 */
	protected MjComponent(IModelInfo modelinfo, ComponentIdentifier id)
	{
		this.modelinfo = modelinfo;
		this.id = id==null? new ComponentIdentifier(): id;
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
		components.put(comp.getId(), comp);
	}
	
	public static void removeComponent(ComponentIdentifier cid)
	{
		components.remove(cid);
	}
	
	public static IComponent getComponent(ComponentIdentifier cid)
	{
		return components.get(cid);
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
	 *  Get a copy of the set of currently instantiated features.
	 *  Does not include lazy, which have not yet been accessed.  
	 */
	public Set<Object>	getFeatures()
	{
		return new LinkedHashSet<>(features.values());
	}
	
	/**
	 *  Get the feature instance for the given type.
	 *  Instantiates lazy features if needed.
	 */
	public <T> T getExistingFeature(Class<T> type)
	{
		//return getFeatures().stream().findFirst(feature -> feature instanceof IMjLifecycle);
		return getFeatures().stream()
	        .filter(feature -> type.isInstance(feature))
	        .map(type::cast)  
	        .findFirst()
	        .orElse(null); 
	}
	
	/**
	 *  Get the feature instance for the given type.
	 *  Instantiates lazy features if needed.
	 */
	public <T> T getFeature(Class<T> type)
	{
		if(features.containsKey(type))
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
	public void terminate()
	{
		IComponent.terminate(id).get();
	}
	
	protected void putFeature(Class<Object> type, Object feature)
	{
		System.out.println("putFeature: "+type+" "+feature);
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
				
				if(!found && ((exact && MjComponent.class.equals(type))
					|| (!exact && SReflect.isSupertype(MjComponent.class, type))))
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
					public <T> IFuture<T> scheduleStep(Supplier<T> step) 
					{
						throw new UnsupportedOperationException("Missing execution feature");
					}
					
					@Override
					public void scheduleStep(Runnable step) 
					{
						throw new UnsupportedOperationException("Missing execution feature");
					}
					
					@Override
					public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
					{
						throw new UnsupportedOperationException("Missing execution feature");
					}
					
					@Override
					public void scheduleStep(IThrowingConsumer<IComponent> step)
					{
						throw new UnsupportedOperationException("Missing execution feature");
					}
					
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
		IExternalAccess access = null;
		if(accessfactory!=null)
		{
			access = accessfactory.apply(cid);
		}
		else
		{
			access = new IExternalAccess() 
			{
				@Override
				public <T> IFuture<T> scheduleStep(Supplier<T> step) 
				{
					throw new UnsupportedOperationException("Missing execution feature");
				}
				
				@Override
				public void scheduleStep(Runnable step) 
				{
					throw new UnsupportedOperationException("Missing execution feature");
				}
				
				@Override
				public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
				{
					throw new UnsupportedOperationException("Missing execution feature");
				}
				
				@Override
				public void scheduleStep(IThrowingConsumer<IComponent> step)
				{
					throw new UnsupportedOperationException("Missing execution feature");
				}
				
				@Override
				public ComponentIdentifier getId() 
				{
					return MjComponent.this.getId();
				}
			};
		}
		
		return access;
	}
	
	/**
	 *  Set the external access factory.
	 *  @param factory The factory.
	 */
	public static void setExternalAccessFactory(Function<Object, IExternalAccess> factory)
	{
		accessfactory = factory;
	}
	
	public ClassLoader getClassLoader()
	{
		return this.getClass().getClassLoader();
	}
}
