package jadex.core.impl;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.core.IComponentHandle;
import jadex.core.IResultProvider;
import jadex.core.ChangeEvent;
import jadex.core.annotation.NoCopy;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;

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
	
	/** The last exception, if any. */
	protected Exception	exception;

	/** Is the component terminated? */
	protected boolean terminated;
	
//	/** The value provider. */
//	protected ValueProvider valueprovider;
	
	/** The external access supplier. */
	protected static Function<Component, IComponentHandle> accessfactory;
		
	/** The is the external access executable, i.e. is scheduleStep allowed?. */
	protected static boolean executable;
	
	/**
	 *  Create a component object to be inited later
	 *  @param pojo The pojo, if any.
	 *  @param id The component id, or null for auto-generation.
	 *  @param app The application context, if any.
	 */
	public Component(Object pojo, ComponentIdentifier id, Application app)
	{
		this.pojo	= pojo;
		this.id = id;
		this.app = app;
	}

	/**
	 *  Initialize the component.
	 *  Instantiate all features (except lazy features).
	 *  @throws IllegalArgumentException when the id already exists. 
	 */
	public void init()
	{
		// If no id is given, create a new one.
		this.id = id==null? new ComponentIdentifier(): id;
		
		//System.out.println(this.id.getLocalName());
		ComponentManager.get().addComponent(this);
		
		// Instantiate all features (except lazy ones).
		// Use getProviderListForComponent as it uses a cached array list
		List<ComponentFeatureProvider<IComponentFeature>>	providers
			= SComponentFeatureProvider.getProviderListForComponent(getClass());
		if(!providers.isEmpty())
		{
			try
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

				// Initialize all features, i.e. non-lazy ones that implement ILifecycle.
				for(Object feature:	getFeatures())
				{
					if(feature instanceof ILifecycle)
					{
						ILifecycle lfeature = (ILifecycle)feature;
						//System.out.println("starting: "+lfeature);
						lfeature.init();
					}
				}
			}
			catch(Throwable t)
			{
				// If an exception occurs, remove the component from the manager.
				try
				{
					terminate();
				}
				catch(StepAborted e)
				{
					// Skip abortion of user code to throw original exception.
				}
				throw SUtil.throwUnchecked(t);
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
		return features!=null ? features.values() : Collections.emptySet();
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
					
					if(ret instanceof ILifecycle)
					{
						ILifecycle lfeature = (ILifecycle)ret;
						//System.out.println("starting: "+lfeature);
						lfeature.init();
					}

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
	 *  Check if the component is terminated.
	 */
	public boolean isTerminated()
	{
		return terminated;
	}
	
	/**
	 *  Terminate the component.
	 */
	public void	terminate()
	{
		if(terminated)
		{
			throw new ComponentTerminatedException(id);
		}
		terminated	= true;
		
		// Terminate all features in reverse creation order.
		if(features!=null)
		{
			// Use getProviderListForComponent as it uses a cached array list
			List<ComponentFeatureProvider<IComponentFeature>>	providers
				= SComponentFeatureProvider.getProviderListForComponent(getClass());
			// TODO: On Exception in feature init(), cleanup() may be called for some features without init() being called.
			// Fixing this is complicated is because lazy features may be created during init() of other features.
			for(int i=providers.size()-1; i>=0; i--)
			{
				ComponentFeatureProvider<IComponentFeature>	provider	= providers.get(i);
				Object feature = features.get(provider.getFeatureType());
				if(feature instanceof ILifecycle) 
				{
					ILifecycle lfeature = (ILifecycle)feature;
					try
					{
						lfeature.cleanup();
					}
					catch(Throwable t2)
					{
						System.getLogger(this.getClass().getName()).log(Level.WARNING, "Error terminating feature: "+lfeature, t2);
					}
				}
			}
		}
		
		// Remove the component from the manager.
		ComponentManager.get().removeComponent(this);
		
		// If running on execution feature -> abort component step to avoid further user code being called.
//		if(ComponentManager.get().getCurrentComponent()==this)
//		{
//			throw new StepAborted(getId());
//		}
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
	
	// TODO: move to model/bpmn
	public ValueProvider getValueProvider()
	{
//		if(valueprovider==null)
//			valueprovider = new ValueProvider(this);
//		return valueprovider;
		return new ValueProvider(this);
	}

	// TODO: move to model/bpmn
	public ClassLoader getClassLoader()
	{
		return pojo!=null ? pojo.getClass().getClassLoader() : getClass().getClassLoader();
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
				access = new BasicComponentHandle();
			}
		}
		return access;
	}
	
	public void handleException(Exception exception)
	{
		this.exception	= exception;
		if(exception instanceof ComponentTerminatedException && this.getId().equals(((ComponentTerminatedException)exception).getComponentIdentifier()))
		{
			System.getLogger(this.getClass().getName()).log(Level.INFO, "Component terminated exception: "+exception);
		}
		else
		{
			@SuppressWarnings("unchecked")
			BiConsumer<Exception, IComponent> handler = (BiConsumer<Exception, IComponent>)ComponentManager.get()
				.getFeature(IErrorHandlingFeature.class)
				.getExceptionHandler(exception, this);
			handler.accept(exception, this);
		}
	}

	/**
	 *  Get the last exception, if any.
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 *  Get the component handle.
	 *  @param cid The component id.
	 *  @return The handle.
	 */
	public IComponentHandle getComponentHandle(ComponentIdentifier cid)
	{
		//return IComponent.getExternalComponentAccess(cid);
		return ComponentManager.get().getComponentHandle(cid);
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
	
	/**
	 *  Initialize a component and register it in the component manager.
	 */
	public static <T extends Component> IFuture<IComponentHandle>	createComponent(T component)
	{
		if (!(component instanceof IDaemonComponent))
		{
			ComponentManager man = ComponentManager.get();
			man.initializeFeatures();
		}

		IBootstrapping	bootstrapping	= SComponentFeatureProvider.getBootstrapping(component.getClass());
		if(bootstrapping!=null)
		{
			return bootstrapping.bootstrap(component);
		}
		else
		{
			Future<IComponentHandle>	ret	= new Future<>();
			try
			{
				component.init();
				ret.setResult(component.getComponentHandle());
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
			return ret;
		}
	}

	@Override
	public String toString() 
	{
		return "Component [id=" + id + "]";
	}
		
	/**
	 *  Fetch the result(s) of the Component.
	 *  Schedules to the component, if not terminated.
	 */
	public static IFuture<Map<String, Object>> getResults(IComponent comp)
	{
		Future<Map<String, Object>>	ret	= new Future<>();
		
		if(isExecutable())
		{
			comp.getComponentHandle().scheduleStep(() -> doGetResults(comp))
				.then(results -> ret.setResult(results))
				.catchEx(e ->
				{
					if(e instanceof ComponentTerminatedException)
					{
						// When terminated, don't schedule.
						ret.setResult(Component.doGetResults(comp));
					}
					else
					{
						ret.setException(e);
					}
				});
		}
		else
		{
			ret.setResult(Component.doGetResults(comp));
		}
		
		return ret;
	}
	
	/**
	 *  Listen to results of the component.
	 *  Schedules to the component, if not terminated.
	 *  @throws UnsupportedOperationException when subscription is not supported
	 */
	public static ISubscriptionIntermediateFuture<ChangeEvent> subscribeToResults(IComponent comp)
	{
		if(isExecutable())
		{
			SubscriptionIntermediateFuture<ChangeEvent>	ret	= new SubscriptionIntermediateFuture<>();
			
			@SuppressWarnings("rawtypes")
			Callable	call	= new Callable<ISubscriptionIntermediateFuture<ChangeEvent>>()
			{
				public ISubscriptionIntermediateFuture<ChangeEvent>	call()
				{
					return doSubscribeToResults(comp);
				}
			};

			@SuppressWarnings("unchecked")
			ISubscriptionIntermediateFuture<ChangeEvent>	fut	= (ISubscriptionIntermediateFuture<ChangeEvent>)
				comp.getComponentHandle().scheduleAsyncStep(call);
			fut.next(res -> ret.addIntermediateResult(res))
				.catchEx(e ->
				{
					if(e instanceof ComponentTerminatedException)
					{
						// -> ignore (no more results available)
					}
					else
					{
						ret.setException(e);
					}
				});
			
			return ret;
		}
		else
		{
			return Component.doSubscribeToResults(comp);
		}
	}
	
	/**
	 *  Listen to results of the pojo.
	 *  To be called on component thread, if any.
	 *  @throws UnsupportedOperationException when subscription is not supported
	 */
	public static ISubscriptionIntermediateFuture<ChangeEvent> doSubscribeToResults(IComponent component)
	{
		ISubscriptionIntermediateFuture<ChangeEvent>	ret	= null;
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
		{
			ret	= new SubscriptionIntermediateFuture<>(new UnsupportedOperationException("Could not get results: "+component.getPojo()));
		}
		
		return ret;
	}

	/**
	 *  Extract the results from a pojo.
	 *  To be called on component thread, if any.
	 *  @return The result map.
	 */
	public static Map<String, Object> doGetResults(IComponent component)
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

	@NoCopy
	public class BasicComponentHandle implements IComponentHandle
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
		public IFuture<Map<String, Object>> getResults()
		{
			return Component.getResults(Component.this);
		}

		@Override
		public ISubscriptionIntermediateFuture<ChangeEvent> subscribeToResults()
		{
			return Component.subscribeToResults(Component.this);
		}
	}
}
