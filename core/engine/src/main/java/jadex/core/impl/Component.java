package jadex.core.impl;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;

/**
 *  Base class for Jadex components, which provides access to component features.
 */
public class Component implements IComponent
{
	/** The feature instances of this component, stored by the feature type. */
	protected Map<Class<IComponentFeature>, IComponentFeature> features;
	
	/** The pojo, if any.*/
	protected Object pojo;
	
	/** The id. */
	protected ComponentIdentifier id;
	
	/** The app id, if any. */
	protected Application app;
	
	/** The external access. */
	protected IComponentHandle access;

	/** Cache for the component logger. */
	protected Logger logger;
	
	/** The value provider. */
	protected ValueProvider valueprovider;
	
	/** The external access supplier. */
	protected static Function<Component, IComponentHandle> accessfactory;
		
	/** The is the external access executable, i.e. is scheduleStep allowed?. */
	protected static boolean executable;
		
	/**
	 *  Create a new component and instantiate all features (except lazy features).
	 *  Uses an auto-generated component identifier.
	 *  @param pojo	The pojo, if any.
	 */
	public Component(Object pojo)
	{
		this(pojo, null, null);
	}
	
	/**
	 *  Create a new component and instantiate all features (except lazy features).
	 *  @param id	The id to use or null for an auto-generated id.
	 *  @throws IllegalArgumentException when the id already exists. 
	 */
	public Component(Object pojo, ComponentIdentifier id)
	{
		this(pojo, id, null);
	}
	
	/**
	 *  Create a new component and instantiate all features (except lazy features).
	 *  @param id	The id to use or null for an auto-generated id.
	 *  @throws IllegalArgumentException when the id already exists. 
	 */
	public Component(Object pojo, ComponentIdentifier id, Application app)
	{
		this.pojo	= pojo;
		this.id = id==null? new ComponentIdentifier(): id;
		this.app = app;
		
		//System.out.println(this.id.getLocalName());
		ComponentManager.get().addComponent(this);
		
		// Instantiate all features (except lazy ones).
		// Use getProviderListForComponent as it uses a cached array list
		List<ComponentFeatureProvider<IComponentFeature>>	providers
			= SComponentFeatureProvider.getProviderListForComponent(getClass());
		if(!providers.isEmpty())
		{
			features = new LinkedHashMap<>(providers.size(), 1);
			for(ComponentFeatureProvider<IComponentFeature> provider: providers)
			{
				if(!provider.isLazyFeature())
				{
					IComponentFeature	feature	= provider.createFeatureInstance(this);
					features.put(provider.getFeatureType(), feature);
				}
			}
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
	 *  Get the application.
	 */
	public Application getApplication()
	{
		return app;
	}

	/**
	 *  Get the app id.
	 *  return The app id.
	 */
	public String getAppId()
	{
		return app!=null? app.getId(): null;
	}

	/**
	 *  Get the internal set of currently instantiated features.
	 *  Does not include lazy, which have not yet been accessed.  
	 */
	public Collection<IComponentFeature>	getFeatures()
	{
		return features!=null ? (Collection<IComponentFeature>)features.values() : Collections.emptySet();
	}
	
	/**
	 *  Check if has a feature.
	 *  @return True, if it has the feature.
	 */
	public boolean hasFeature(Class<?> type)
	{
		return features!=null && features.containsKey(type);
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
	public <T extends IComponentFeature> T getFeature(Class<T> type)
	{
		if(features!=null && features.containsKey(type))
		{
			@SuppressWarnings("unchecked")
			T	ret	= (T)features.get(type);
			return ret;
		}
		else
		{
			Map<Class<IComponentFeature>, ComponentFeatureProvider<IComponentFeature>>	providers
				= SComponentFeatureProvider.getProvidersForComponent(getClass());
			if(providers.containsKey(type))
			{
				try
				{
					ComponentFeatureProvider<?>	provider	= providers.get(type);
					assert provider.isLazyFeature();
					@SuppressWarnings("unchecked")
					T ret = (T)provider.createFeatureInstance(this);
					@SuppressWarnings("rawtypes")
					Class rtype	= type;
					@SuppressWarnings("unchecked")
					Class<IComponentFeature> otype	= (Class<IComponentFeature>)rtype;
					features.put(otype, ret);
					return ret;
				}
				catch(Throwable t)
				{
					throw SUtil.throwUnchecked(t);
				}
			}
			else
			{
				throw new RuntimeException("No such feature: "+type);
			}
		}
	}
	
	/**
	 *  Terminate the component.
	 */
	public IFuture<Void> terminate(ComponentIdentifier... cids)
	{
		if(cids.length==0)
		{
			ComponentManager.get().removeComponent(this.getId());
			
			IComponentLifecycleManager	creator	= SComponentFeatureProvider.getCreator(getClass());
			if(creator!=null)
			{
				creator.terminate(this);
			}
			else
			{
				throw new RuntimeException("Cannot terminate component of type: "+getClass());
			}
				
			return IFuture.DONE;
		}
		else
		{
			FutureBarrier<Void> bar = new FutureBarrier<Void>();
			for(ComponentIdentifier cid: cids)
			{
				bar.add(IComponentManager.get().terminate(cid));
			}
			return bar.waitFor();
		}
		
//		throw new UnsupportedOperationException("No termination code for component: "+getId());
	}
	
	/**
	 *  Get the pojo.
	 *  @return The pojo.
	 */
	public Object getPojo()
	{
		return pojo;
	}

	/**
	 *  Returns the appropriate logging access for the component.
	 *
	 *  @return The component logger.
	 */
	public Logger getLogger()
	{
		if (logger == null)
			logger = System.getLogger(pojo != null ? pojo.getClass().getName() : getId().getLocalName());
		return logger;
	}
	
	public ValueProvider getValueProvider()
	{
		if(valueprovider==null)
			valueprovider = new ValueProvider(this);
		return valueprovider;
	}
	
	/*public Map<String, Object> getResults(Object pojo)
	{
		return Collections.EMPTY_MAP;
	}*/
	
	/* *
	 *  Get the fetcher.
	 *  @return The fetcher.
	 * /
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
					}* /
					
					if(!found)
						throw new RuntimeException("Value not found: "+name);
//					else
//						System.out.println("fetcher: "+name+" "+ret);
					
					return ret;
				}
			};
		}
		
		return fetcher;
	}*/
	
	/*
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
				}* /
				
				if(!found)
					throw new RuntimeException("Value not found: "+type);
				
				return ret;
			}
			
		};
	}*/
	
	/**
	 *  Get the external access.
	 *  @return The external access.
	 */
	public IComponentHandle getComponentHandle()
	{
		if(access==null)
		{
			if(accessfactory!=null)
			{
				access = accessfactory.apply(this);
			}
			else
			{
				access = new IComponentHandle() 
				{
					@Override
					public ComponentIdentifier getId() 
					{
						return Component.this.getId();
					}
					
					@Override
					public String getAppId() 
					{
						return Component.this.getAppId();
					}
					
					@Override
					public <T> T getPojoHandle(Class<T> type)
					{
						throw new UnsupportedOperationException();
					}
				};
			}
		}
		return access;
	}
	
	public void handleException(Exception exception)
	{
		if(exception instanceof ComponentTerminatedException && this.getId().equals(((ComponentTerminatedException)exception).getComponentIdentifier()))
		{
			System.getLogger(this.getClass().getName()).log(Level.INFO, "Component terminated exception: "+exception);
		}
		else
		{
			@SuppressWarnings("unchecked")
			BiConsumer<Exception, IComponent> handler = (BiConsumer<Exception, IComponent>)ComponentManager.get().getFeature(IErrorHandlingFeature.class).getExceptionHandler(exception, this);
			handler.accept(exception, this);
		}
	}
	
	/**
	 *  Get the component handle.
	 *  @param cid The component id.
	 *  @return The handle.
	 */
	public IComponentHandle getComponentHandle(ComponentIdentifier cid)
	{
		//return IComponent.getExternalComponentAccess(cid);
		return ComponentManager.get().getComponent(cid).getComponentHandle();
	}
	
	/**
	 *  Set the external access factory.
	 *  @param factory The factory.
	 *  @param executable	Is scheduleStep() allowed on external access?
	 */
	public static void setExternalAccessFactory(Function<Component, IComponentHandle> factory, boolean executable)
	{
		accessfactory = factory;
		Component.executable	= executable;
	}
	
	/**
	 *  Is scheduleStep() allowed on external access?
	 */
	public static boolean	isExecutable()
	{
		return executable;
	}
	
	// TODO move to model feature?
	public ClassLoader getClassLoader()
	{
		return this.getClass().getClassLoader();
	}

	public static <T extends Component> T createComponent(Class<T> type, Supplier<T> creator)
	{
		List<ComponentFeatureProvider<IComponentFeature>>	providers	= SComponentFeatureProvider.getProviderListForComponent(type);
		for(int i=providers.size()-1; i>=0; i--)
		{
			ComponentFeatureProvider<IComponentFeature>	provider	= providers.get(i);
			if(provider instanceof IBootstrapping)
			{
				Supplier<T>	nextcreator	= creator;
				creator	= () -> ((IBootstrapping)provider).bootstrap(type, nextcreator);
			}
		}
		return creator.get();
	}

	@Override
	public String toString() 
	{
		return "Component [id=" + id + "]";
	}
}
