package jadex.providedservice.impl.service;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

import jadex.bytecode.ProxyFactory;
import jadex.common.IValueFetcher;
import jadex.common.MethodInfo;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.UnparsedExpression;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.execution.impl.ILifecycle;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;
import jadex.model.IModelFeature;
import jadex.model.modelinfo.ModelInfo;
import jadex.providedservice.IMethodInvocationListener;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.ServiceComponent;
import jadex.providedservice.impl.search.IServiceRegistry;
import jadex.providedservice.impl.search.ServiceRegistry;
import jadex.providedservice.impl.service.interceptors.DecouplingInterceptor;
import jadex.providedservice.impl.service.interceptors.DecouplingReturnInterceptor;
import jadex.providedservice.impl.service.interceptors.MethodCallListenerInterceptor;
import jadex.providedservice.impl.service.interceptors.MethodInvocationInterceptor;
import jadex.providedservice.impl.service.interceptors.ResolveInterceptor;

public abstract class ProvidedServiceFeature implements ILifecycle, IProvidedServiceFeature//, IParameterGuesser
{
	protected Component self;
	
	/** The map of platform services. */
	protected Map<Class<?>, Collection<IInternalService>> services;
	
	/** The map of provided service infos. (sid -> provided service info) */
	protected Map<IServiceIdentifier, ProvidedServiceInfo> serviceinfos;
	
	/** The map of provided service infos. (sid -> method listener) */
	protected Map<IServiceIdentifier, MethodListenerHandler> servicelisteners;
	
	/** The pojo service map (pojo -> proxy). */
	protected static Map<Object, IService>	pojoproxies;

	
	protected ProvidedServiceFeature(Component self)
	{
		this.self	= self;
	}
	
	public ProvidedServiceModel loadModel()
	{
		return null;
	}
	
	@Override
	public void	onStart()
	{
		ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();
		ProvidedServiceModel mymodel = (ProvidedServiceModel)model.getFeatureModel(IProvidedServiceFeature.class);
		if(mymodel==null)
			mymodel = loadModel();
		
		if(mymodel!=null)
		{
			// Collect provided services from model (name or type -> provided service info)
			//ProvidedServiceInfo[] ps = (ProvidedServiceInfo[])model.getFeatureModel(IMjProvidedServiceFeature.class);
			ProvidedServiceInfo[] ps = mymodel.getServices();
			Map<Object, ProvidedServiceInfo> sermap = new LinkedHashMap<Object, ProvidedServiceInfo>();
			if(ps!=null)
			{
				for(int i=0; i<ps.length; i++)
				{
					Object key = ps[i].getName()!=null? ps[i].getName(): ps[i].getType().getType(self.getClass().getClassLoader(), model.getAllImports());
					if(sermap.put(key, ps[i])!=null)
					{
						throw new RuntimeException("Services with same type must have different name.");
					}
				}
			}
			else
			{
				System.out.println("has no services: "+self);
			}
					
			// Adapt services to configuration (if any).
			/*
			if(component.getConfiguration()!=null)
			{
				ConfigurationInfo cinfo = component.getModel().getConfiguration(component.getConfiguration());
				ProvidedServiceInfo[] cs = cinfo.getProvidedServices();
				for(int i=0; i<cs.length; i++)
				{
					Object key = cs[i].getName()!=null? cs[i].getName(): cs[i].getType().getType(component.getClassLoader(), component.getModel().getAllImports());
					ProvidedServiceInfo psi = (ProvidedServiceInfo)sermap.get(key);
					ProvidedServiceInfo newpsi= new ProvidedServiceInfo(psi.getName(), psi.getType().getType(component.getClassLoader(), component.getModel().getAllImports()), 
						new ProvidedServiceImplementation(cs[i].getImplementation()), 
						cs[i].getScope()!=null? cs[i].getScope(): psi.getScope(),
						cs[i].getScopeExpression()!=null? cs[i].getScopeExpression(): psi.getScopeExpression(),
						cs[i].getSecurity()!=null? cs[i].getSecurity(): psi.getSecurity(),
						cs[i].getPublish()!=null? cs[i].getPublish(): psi.getPublish(), 
						cs[i].getProperties()!=null? cs[i].getProperties() : psi.getProperties());
					sermap.put(key, newpsi);
				}
			}*/
					
			// Add custom service infos from outside.
			/*
			ProvidedServiceInfo[] pinfos = cinfo.getProvidedServiceInfos();
			for(int i=0; pinfos!=null && i<pinfos.length; i++)
			{
				Object key = pinfos[i].getName()!=null? pinfos[i].getName(): pinfos[i].getType().getType(component.getClassLoader(), component.getModel().getAllImports());
				ProvidedServiceInfo psi = (ProvidedServiceInfo)sermap.get(key);
				ProvidedServiceInfo newpsi= new ProvidedServiceInfo(psi.getName(), psi.getType().getType(component.getClassLoader(), component.getModel().getAllImports()), 
					pinfos[i].getImplementation()!=null? new ProvidedServiceImplementation(pinfos[i].getImplementation()): psi.getImplementation(), 
					pinfos[i].getScope()!=null? pinfos[i].getScope(): psi.getScope(),
					pinfos[i].getScopeExpression()!=null? pinfos[i].getScopeExpression(): psi.getScopeExpression(),
					pinfos[i].getSecurity()!=null? pinfos[i].getSecurity(): psi.getSecurity(),
					pinfos[i].getPublish()!=null? pinfos[i].getPublish(): psi.getPublish(), 
					pinfos[i].getProperties()!=null? pinfos[i].getProperties() : psi.getProperties());
				sermap.put(key, newpsi);
			}*/
					
			// Add external access service when not turned off
			/*		
			Map<String, Object> args = getComponent().getInternalAccess().getArguments();
			Boolean extaarg = (Boolean)args.get("externalaccess");
			Boolean extaplatarg = (Boolean)((Map<String, Object>)Starter.getPlatformValue(getComponent().getId(), IPlatformConfiguration.PLATFORMARGS)).get("externalaccess");
			boolean on = extaarg!=null? extaarg.booleanValue(): extaplatarg!=null? extaplatarg.booleanValue(): true;
	//					System.out.println("on: "+on+" "+extaarg+" "+extaplatarg);
			if(on)
			{
				ProvidedServiceImplementation impl = new ProvidedServiceImplementation();
				impl.setValue("$component.getExternalAccess()");
				// platform external access service will be published network wide, all others only on platform
				ProvidedServiceInfo psi= new ProvidedServiceInfo("externalaccessservice", IExternalAccess.class, impl, 
					getComponent().getId().equals(getComponent().getId().getRoot())? ServiceScope.NETWORK: ServiceScope.PLATFORM, null, null, null, null);
				sermap.put("externalaccessservice", psi);
			}*/
					
			FutureBarrier<Void> bar = new FutureBarrier<>();
			
			// Instantiate service objects
			for(ProvidedServiceInfo info: sermap.values())
			{
				// Evaluate and replace scope expression, if any.
				//ServiceScope scope = info.getScope();
				/*if(ServiceScope.EXPRESSION.equals(scope))
				{
					scope = (ServiceScope)SJavaParser.getParsedValue(info.getScopeExpression(), model.getAllImports(), self.getFeature(IModelFeature.class).getFetcher(), self.getClassLoader());
					info = new ProvidedServiceInfo(info.getName(), info.getType(), info.getImplementation(), scope, info.getScopeExpression(), info.getSecurity(), 
							//info.getPublish(), 
							info.getProperties(), info.isSystemService());
	//						System.out.println("expression scope '"
	//							+ (info.getScopeExpression()!=null ? info.getScopeExpression().getValue() : "")
	//							+ "': "+scope);
				}*/
					
				final Future<Void> fut = new Future<>();
				bar.add(fut);
				
				//final ProvidedServiceImplementation	impl = info.getImplementation();
				// Virtual service (e.g. promoted)
				/*if(impl!=null && impl.getBinding()!=null)
				{
					RequiredServiceInfo rsi = new RequiredServiceInfo(BasicService.generateServiceName(info.getType().getType( 
						component.getClassLoader(), component.getModel().getAllImports()))+":virtual", info.getType().getType(component.getClassLoader(), component.getModel().getAllImports()));
					IServiceIdentifier sid = BasicService.createServiceIdentifier(component, 
						rsi.getName(), rsi.getType().getType(component.getClassLoader(), component.getModel().getAllImports()),
						BasicServiceInvocationHandler.class, component.getModel().getResourceIdentifier(), info);
					final IInternalService service = BasicServiceInvocationHandler.createDelegationProvidedServiceProxy(
						component, sid, rsi, impl.getBinding(), component.getClassLoader(), Starter.isRealtimeTimeout(component.getId(), true));
					
					addService(service, info);
					fut.setResult(null);
				}
				else
				{*/
					final ProvidedServiceInfo finfo = info;
					createServiceImplementation(info, self.getValueProvider().getFetcher())
						.then(ser ->
					{
						// Implementation may null to disable service in some configurations.
						if(ser!=null)
						{
							UnparsedExpression[] ins = finfo.getImplementation().getInterceptors();
							IServiceInvocationInterceptor[] ics = null;
							if(ins!=null)
							{
								ics = new IServiceInvocationInterceptor[ins.length];
								for(int i=0; i<ins.length; i++)
								{
									if(ins[i].getValue()!=null && ins[i].getValue().length()>0)
									{
										ics[i] = (IServiceInvocationInterceptor)SJavaParser.evaluateExpression(ins[i].getValue(), self.getFeature(IModelFeature.class).getModel().getAllImports(), self.getValueProvider().getFetcher(), self.getClassLoader());
									}
									else
									{
										try
										{
											ics[i] = (IServiceInvocationInterceptor)ins[i].getClazz().getType(self.getClassLoader(), self.getFeature(IModelFeature.class).getModel().getAllImports()).newInstance();
										}
										catch(Exception e)
										{
											e.printStackTrace();
										}
									}
								}
							}
							
							final Class<?> type = finfo.getType().getType(self.getClassLoader(), self.getFeature(IModelFeature.class).getModel().getAllImports());
							//PublishEventLevel elm = component.getDescription().getMonitoring()!=null? component.getDescription().getMonitoring(): null;
	//								 todo: remove this? currently the level cannot be turned on due to missing interceptor
							//boolean moni = elm!=null? !PublishEventLevel.OFF.equals(elm.getLevel()): false; 
							final IInternalService proxy = this.createProvidedServiceProxy(
								self, ser, finfo.getName(), type, ics,
								//moni, 
								finfo);
							
							addService(proxy, finfo);
						}
						fut.setResult(null);
						
					}).catchEx(e -> {e.printStackTrace(); fut.setResult(null);});
				//}
			}
			
			bar.waitFor().get();
			// Start the services.
			Collection<IInternalService> allservices = getAllServices();
			if(!allservices.isEmpty())
			{
				initServices(allservices.iterator()).get();
			}
		}
	}
	
	/**
	 *  Called when the feature is shutdowned.
	 */
	public void	onEnd()
	{
		// Shutdown the services.
		Collection<IInternalService> allservices = getAllServices();
		if(!allservices.isEmpty())
		{
			LinkedList<IInternalService> list = new LinkedList<IInternalService>(allservices);
			shutdownServices(list.descendingIterator()).get();
		}
	}
	
	/**
	 *  Shutdown the services one by one.
	 */
	protected IFuture<Void> shutdownServices(final Iterator<IInternalService> services)
	{
		final Future<Void> ret = new Future<Void>();
		if(services.hasNext())
		{
			final IInternalService	is	= services.next();
			// Remove service from registry before shutdown.
			removeService(is);
			
//			component.getLogger().info("Stopping service: "+is.getServiceId());
//			if(is instanceof IExternalAccess)
//				System.out.println("Stopping service: "+is.getServiceId());
			is.shutdownService().addResultListener(new DelegationResultListener<Void>(ret)
			{
				public void customResultAvailable(Void result)
				{
//					component.getLogger().info("Stopped service: "+is.getServiceId());
//					System.out.println("Stopped service: "+is.getServiceId());
					serviceShutdowned(is).addResultListener(new DelegationResultListener<Void>(ret)
					{
						public void customResultAvailable(Void result)
						{
							shutdownServices(services).addResultListener(new DelegationResultListener<Void>(ret));
						}
					});
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					// On error -> print and continue shutdown process.
					System.out.println("Exception in service shutdown: "+is+"\n"+SUtil.getExceptionStacktrace(exception));
					customResultAvailable(null); 
				}
			});
		}
		else
		{
			ret.setResult(null);
		}
		return ret;
	}
	
	/**
	 *  Remove a service.
	 *  @param service	The service object.
	 *  @param info	 The service info.
	 */
	protected void removeService(IInternalService service)
	{
		IServiceRegistry registry = ServiceRegistry.getRegistry();
		registry.removeService(service.getServiceId());
	}
	
	/**
	 *  Called after a service has been shutdowned.
	 *  
	 *  todo: add code to unpublish in publication feature!
	 */
	public IFuture<Void> serviceShutdowned(final IInternalService service)
	{
		final Future<Void> ret = new Future<Void>();
//		adapter.invokeLater(new Runnable()
//		{
//			public void run()
//			{
				ProvidedServiceInfo info = getProvidedServiceInfo(service.getServiceId());
				
				// todo!!!
				//final PublishInfo pi = info==null? null: info.getPublish();
				
//				System.out.println("shutdown ser: "+service.getId());
				/*if(pi!=null)
				{
					final IServiceIdentifier sid = service.getServiceId();
//					getPublishService(instance, pi.getPublishType(), null).addResultListener(instance.createResultListener(new IResultListener<IPublishService>()
					getPublishService(getComponent(), pi.getPublishType(), pi.getPublishScope(), null).addResultListener(new IResultListener<IPublishService>()
					{
						public void resultAvailable(IPublishService ps)
						{
							ps.unpublishService(sid).addResultListener(new DelegationResultListener<Void>(ret));
						}
						
						public void exceptionOccurred(Exception exception)
						{
			//				instance.getLogger().severe("Could not unpublish: "+sid+" "+exception.getMessage());
							
							// ignore, if no publish info
							ret.setResult(null);
							// todo: what if publish info but no publish service?
						}
					});
				}
				else
				{
					ret.setResult(null);
				}*/				
//			}
//		});
		//return ret;
	
		return IFuture.DONE;
	}
	
	/**
	 *  Get the publish service for a publish type (e.g. web service).
	 *  @param type The type.
	 *  @param services The iterator of publish services (can be null).
	 *  @return The publish service.
	 * /
	public static IFuture<IPublishService> getPublishService(final MjComponent instance, final String type, final ServiceScope scope, final Iterator<IPublishService> services)
	{
		final Future<IPublishService> ret = new Future<IPublishService>();
		
		if(services==null)
		{
			IFuture<Collection<IPublishService>> fut = instance.getFeature(IRequiredServicesFeature.class).searchServices(new ServiceQuery<>(IPublishService.class, scope));
			fut.addResultListener(instance.getFeature(IExecutionFeature.class).createResultListener(new ExceptionDelegationResultListener<Collection<IPublishService>, IPublishService>(ret)
			{
				@Override
				public void exceptionOccurred(Exception exception) {
					// TODO Auto-generated method stub
					super.exceptionOccurred(exception);
					exception.printStackTrace();
				}
				public void customResultAvailable(Collection<IPublishService> result)
				{
					getPublishService(instance, type, scope, result.iterator()).addResultListener(new DelegationResultListener<IPublishService>(ret));
				}
			}));
		}
		else
		{
			if(services.hasNext())
			{
				final IPublishService ps = (IPublishService)services.next();
				ps.isSupported(type).addResultListener(instance.getFeature(IExecutionFeature.class).createResultListener(new ExceptionDelegationResultListener<Boolean, IPublishService>(ret)
				{
					public void customResultAvailable(Boolean supported)
					{
						if(supported.booleanValue())
						{
							ret.setResult(ps);
						}
						else
						{
							getPublishService(instance, type, scope, services).addResultListener(new DelegationResultListener<IPublishService>(ret));
						}
					}
				}));
			}
			else
			{
//				ret.setResult(null);
				ret.setException(new ServiceNotFoundException("IPublishService not found."));
			}
		}
		
		return ret;
	}*/
	
	/**
	 *  Create a service implementation from description.
	 */
	public IFuture<Object> createServiceImplementation(ProvidedServiceInfo info, IValueFetcher fetcher) 
	{
		final Future<Object> ret = new Future<>();
		
		Object	ser	= null;
		ProvidedServiceImplementation impl = info.getImplementation();
		if(impl!=null && impl.getValue()!=null)
		{
			// todo: other Class imports, how can be found out?
			try
			{
//				SimpleValueFetcher fetcher = new SimpleValueFetcher(component.getFetcher());
//				fetcher.setValue("$servicename", info.getName());
//				fetcher.setValue("$servicetype", info.getType().getType(component.getClassLoader(), component.getModel().getAllImports()));
//				System.out.println("sertype: "+fetcher.fetchValue("$servicetype")+" "+info.getName());
				ser = SJavaParser.getParsedValue(impl, self.getFeature(IModelFeature.class).getModel().getAllImports(), fetcher, self.getClassLoader());
//				System.out.println("added: "+ser+" "+model.getName());
				ret.setResult(ser);
			}
			catch(RuntimeException e)
			{
//				e.printStackTrace();
				ret.setException(new RuntimeException("Service creation error: "+info, e));
			}
		}
		else if(impl!=null && impl.getClazz()!=null)
		{
			if(impl.getClazz().getType(self.getClassLoader(), self.getFeature(IModelFeature.class).getModel().getAllImports())!=null)
			{
				try
				{
					ser = impl.getClazz().getType(self.getClassLoader(), self.getFeature(IModelFeature.class).getModel().getAllImports()).getDeclaredConstructor().newInstance();
					ret.setResult(ser);
				}
				catch(Exception e)
				{
					ret.setException(e);
				}
			}
			else
			{
				ret.setException(new RuntimeException("Service class not found: "+impl.getClazz()));
			}
			/*else
			{
				try
				{
					Class<?> c = Class.forName("jadex.extension.rs.publish.JettyRestPublishService", false, self.getClassLoader());
					System.out.println("foundd: "+c);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				ret.setException(new RuntimeException("Could not load service implementation class: "+impl.getClazz()+" "+self.getClassLoader()));
			}*/
		}
		else if(impl!=null && self.getPojo()!=null)
		{
			ret.setResult(self.getPojo());
		}
		else
		{
			ret.setResult(null);
		}
//		else if(IExternalAccess.class.equals(info.getType().getType(getComponent().getClassLoader())))
//		{
//			ser = getComponent().getExternalAccess();
//		}
		
		return ret;
	}
	
	/**
	 *  Add a service.
	 *  @param service	The service object.
	 *  @param info	 The service info.
	 */
	protected void addService(IInternalService service, ProvidedServiceInfo info)
	{
		if(serviceinfos==null)
			serviceinfos = new HashMap<IServiceIdentifier, ProvidedServiceInfo>();
		serviceinfos.put(service.getServiceId(), info);
		
		// todo: !!!
		
		// Find service types
//		Class<?>	type	= info.getType().getType(component.getClassLoader(), component.getModel().getAllImports());
		/*Class<?> type = service.getServiceId().getServiceType().getType(self.getClassLoader(), self.getModel().getAllImports());
		Set<Class<?>> types = new LinkedHashSet<Class<?>>();
		types.add(type);
		for(Class<?> sin: SReflect.getSuperInterfaces(new Class[]{type}))
		{
			if(sin.isAnnotationPresent(Service.class))
			{
				types.add(sin);
			}
		}*/

		if(services==null)
			services = Collections.synchronizedMap(new LinkedHashMap<Class<?>, Collection<IInternalService>>());
		
//		return ServiceRegistry.getRegistry(component.getComponentIdentifier()).addService(service);
		
//		FutureBarrier<Void> bar = new FutureBarrier<Void>();
		
		// hack!!!!
		Class<?> servicetype = service.getServiceId().getServiceType().getType(this.getClass().getClassLoader());//service.getClass();
				
		//for(Class<?> servicetype: types)
		{
			Collection<IInternalService> tmp = services.get(servicetype);
			if(tmp==null)
			{
				tmp = Collections.synchronizedList(new ArrayList<IInternalService>());
				services.put(servicetype, tmp);
			}
			tmp.add(service);
			
			// Make service available immediately, even before start (hack???).
//			bar.addFuture(SynchronizedServiceRegistry.getRegistry(component.getComponentIdentifier()).addService(new ClassInfo(servicetype), service));
		}
		
		
		ServiceRegistry.getRegistry().addLocalService(service);
		
		//System.out.println("added service: "+component.getId()+" "+service.getServiceId());
//		return bar.waitFor();
	}
	
	/**
	 *  Get all services in a single collection.
	 */
	protected Collection<IInternalService>	getAllServices()
	{
		Collection<IInternalService> allservices;
		if(services!=null && services.size()>0)
		{
			allservices = new LinkedHashSet<IInternalService>();
			for(Iterator<Collection<IInternalService>> it=services.values().iterator(); it.hasNext(); )
			{
				// Service may occur at different positions if added with more than one interface
				Collection<IInternalService> col = it.next();
				for(IInternalService ser: col)
				{
					if(!allservices.contains(ser))
					{
						allservices.add(ser);
					}
				}
			}
		}
		else
		{
			allservices	= Collections.emptySet();
		}
		
		return allservices;
	}
	
	/**
	 *  Init the services one by one.
	 */
	protected IFuture<Void> initServices(final Iterator<IInternalService> services)
	{
		final Future<Void> ret = new Future<Void>();
		if(services.hasNext())
		{
			final IInternalService	is	= services.next();
			initService(is).addResultListener(new DelegationResultListener<Void>(ret)
			{
				public void customResultAvailable(Void result)
				{
					initServices(services).addResultListener(new DelegationResultListener<Void>(ret));
				}
			});
//			component.getLogger().info("Starting service: "+is.getId());
//			is.setComponentAccess(component).addResultListener(new DelegationResultListener<Void>(ret)
//			{
//				public void customResultAvailable(Void result)
//				{
//					is.startService().addResultListener(new IResultListener<Void>()
//					{
//						public void resultAvailable(Void result)
//						{
//							component.getLogger().info("Started service: "+is.getId());
//							
//							
//						}
//						
//						public void exceptionOccurred(Exception exception)
//						{
//							ret.setException(exception);
//						}
//					});
//				}
//			});
		}
		else
		{
			ret.setResult(null);
		}
		return ret;
	}
	
	/**
	 *  Init a service, i.e. set the component (internal access) and call startService.
	 */
	protected IFuture<Void> initService(final IInternalService is)
	{
		final Future<Void> ret = new Future<Void>();
		//System.out.println("Starting service: "+is.getServiceId()+" "+self.getFeature(IMjExecutionFeature.class).isComponentThread());
		IFuture<Void> fut = is.setComponentAccess(self);
		fut.addResultListener(new DelegationResultListener<Void>(ret)
		{
			public void customResultAvailable(Void result)
			{
//				System.out.println("Starting service: "+is.getServiceId()+" "+component.getFeature(IExecutionFeature.class).isComponentThread());
				is.startService().addResultListener(new DelegationResultListener<Void>(ret)
				{
					public void customResultAvailable(Void result)
					{
						//System.out.println("Started service: "+is.getServiceId());
						serviceStarted(is).addResultListener(new DelegationResultListener<Void>(ret));
					}
				});
			}
		});
		return ret;
	}
	
	/**
	 *  Called after a service has been started.
	 */
	public IFuture<Void> serviceStarted(final IInternalService service)
	{
		final Future<Void> ret = new Future<Void>();
		ProvidedServiceInfo info = getProvidedServiceInfo(service.getServiceId());
		//PublishInfo pit = info==null? null: info.getPublish();
		
		//if(pit!=null)
		{
			// Hack?! evaluate the publish id string 
			// Must clone info to not change the model
			/*final PublishInfo pi = new PublishInfo(pit);
			try
			{
				String pid = (String)SJavaParser.evaluateExpression(pi.getPublishId(), getComponent().getModel().getAllImports(), getComponent().getFetcher(), getComponent().getClassLoader());
				pi.setPublishId(pid);
				System.out.println("pid is now: "+pid);
			}
			catch(Exception e)
			{
//				e.printStackTrace();
			}*/
			
			/*if(pi.isMulti())
			{
				getComponent().getFeature(IRequiredServicesFeature.class)
					.searchServices(new ServiceQuery<>(IPublishService.class, pi.getPublishScope()))
					.addResultListener(new IntermediateEmptyResultListener<IPublishService>()
				{
					/** Flag if published at least once. * /
					protected boolean published = false;
					
					/** Flag if finished. * /
					protected boolean finished = false;
					
					public void exceptionOccurred(Exception exception)
					{
						exception.printStackTrace();
					}

					public void intermediateResultAvailable(final IPublishService result)
					{
						result.publishService(service.getServiceId(), pi).addResultListener(new IResultListener<Void>()
						{
							public void resultAvailable(Void vresult)
							{
								if (!published)
								{
									ret.setResult(null);
									published = true;
								}
							}
							
							public void exceptionOccurred(Exception exception)
							{
								if (finished && !published)
								{
									System.out.println("Could not publish: "+service.getServiceId());
									ret.setException(exception);
								}
							}
						});
					}

					public void finished()
					{
						finished = true;
					}
				});*/
//				SServiceProvider.getServices(getComponent(), IPublishService.class, pi.getPublishScope()).addResultListener(new IResultListener<Collection<IPublishService>>()
//				{
//					public void exceptionOccurred(Exception exception)
//					{
//						getComponent().getLogger().severe("Could not publish: "+service.getId()+" "+exception.getMessage());
//						ret.setResult(null);
//					}
//					
//					public void resultAvailable(Collection<IPublishService> result)
//					{
//						for (final IPublishService pubserv : result)
//						{
//							pubserv.publishService(service.getId(), pi).addResultListener(new IResultListener<Void>()
//							{
//								public void resultAvailable(Void result)
//								{
//								}
//								
//								public void exceptionOccurred(Exception exception)
//								{
//									getComponent().getLogger().severe("Could not publish to " + pubserv + ": "+service.getId()+" "+exception.getMessage());
//								}
//							});
//						}
//					}
//				});
			/*}
			else
			{
				getPublishService(getInternalAccess(), pi.getPublishType(), pi.getPublishScope(), (Iterator<IPublishService>)null)
					.addResultListener(getComponent().getFeature(IExecutionFeature.class)
					.createResultListener(new ExceptionDelegationResultListener<IPublishService, Void>(ret)
				{
					public void customResultAvailable(IPublishService ps)
					{
						//System.out.println("Got publish service " + ps);
						ps.publishService(service.getServiceId(), pi)
							.addResultListener(getComponent().getFeature(IExecutionFeature.class).createResultListener(new DelegationResultListener<Void>(ret)));
					}
					public void exceptionOccurred(Exception exception)
					{
	//					exception.printStackTrace();
						getComponent().getLogger().severe("Could not publish: "+service.getServiceId()+" "+exception.getMessage());
						ret.setResult(null);
					}
				}));
			}*/
		}
		//else
		{
			ret.setResult(null);
		}
		return ret;
	}
	
	/**
	 *  Get provided (declared) service.
	 *  @param clazz The interface.
	 *  @return The service.
	 */
	public <T> T[] getProvidedServices(Class<T> clazz)
	{
		Collection<IInternalService> coll	= null;
		if(services!=null)
		{
			if(clazz!=null)
			{
				coll = services.get(clazz);
			}
			else
			{
				coll = new HashSet<IInternalService>();
				for(Class<?> cl: services.keySet())
				{
					Collection<IInternalService> sers = services.get(cl);
					coll.addAll(sers);
				}
			}			
		}
		
		T[] ret	= (T[])Array.newInstance(clazz==null? Object.class: clazz, coll!=null ? coll.size(): 0);
		return coll==null ? ret : coll.toArray(ret);
	}
	
	/**
	 *  Get provided (declared) service.
	 *  @return The service.
	 */
	public IService getProvidedService(String name)
	{
		IService ret = null;
		if(services!=null)
		{
			for(Iterator<Class<?>> it=services.keySet().iterator(); it.hasNext() && ret==null; )
			{
				Collection<IInternalService> sers = services.get(it.next());
				for(Iterator<IInternalService> it2=sers.iterator(); it2.hasNext() && ret==null; )
				{
					IService ser = it2.next();
					if(ser.getServiceId().getServiceName().equals(name))
					{
						ret = ser;
					}
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get the provided service implementation object by id.
	 *  
	 *  @param name The service identifier.
	 *  @return The service.
	 */
	public Object getProvidedService(IServiceIdentifier sid)
	{
		Object ret = null;
		
		Object[] services = getProvidedServices(sid.getServiceType().getType(getComponent().getClassLoader()));
		if(services!=null)
		{
			for(Object ser: services)
			{
				// Special case for fake proxies, i.e. creating a service proxy for a known component (without knowing cid)
				if(sid.getServiceName().equals("NULL"))
				{
					((IService)ser).getServiceId().getServiceType().equals(sid.getServiceType());
					ret = (IService)ser;
					break;
				}
				else if(((IService)ser).getServiceId().equals(sid))
				{
					ret = (IService)ser;
					break;
				}
			}
		}
		
		return ret;	
	}
	
	/**
	 *  Get the provided service implementation object by name.
	 *  
	 *  @param name The service name.
	 *  @return The service.
	 */
	public Object getProvidedServiceRawImpl(String name)
	{
		Object ret = null;
		
		Object service = getProvidedService(name);
		if(service!=null)
		{
			AbstractServiceInvocationHandler handler = (AbstractServiceInvocationHandler)ProxyFactory.getInvocationHandler(service);
			ret = handler.getDomainService();
		}
		
		return ret;	
	}
	
	/**
	 *  Get the raw implementation of the provided service.
	 *  @param clazz The class.
	 *  @return The raw object.
	 */
	public <T> T getProvidedServiceRawImpl(Class<T> clazz)
	{
		T ret = null;
		
		T service = getProvidedService(clazz);
		if(service!=null)
		{
			AbstractServiceInvocationHandler handler = (AbstractServiceInvocationHandler)ProxyFactory.getInvocationHandler(service);
			ret = clazz.cast(handler.getDomainService());
		}
		
		return ret;
	}
	
	/**
	 *  Get provided (declared) service.
	 *  @param clazz The interface.
	 *  @return The service.
	 */
	public <T> T getProvidedService(Class<T> clazz)
	{
		T[] ret = getProvidedServices(clazz);
		return ret.length>0? ret[0]: null;
	}
	
	/**
	 *  Get the provided service implementation object by id.
	 *  
	 *  @param name The service identifier.
	 *  @return The service.
	 */
	public Object getProvidedServiceRawImpl(IServiceIdentifier sid)
	{
		Object ret = null;
		
		Object[] services = getProvidedServices(sid.getServiceType().getType(getComponent().getClassLoader()));
		if(services!=null)
		{
			IService service = null;
			for(Object ser: services)
			{
				if(((IService)ser).getServiceId().equals(sid))
				{
					service = (IService)ser;
					break;
				}
			}
			if(service!=null)
			{
				if(ProxyFactory.isProxyClass(service.getClass()))
				{
					AbstractServiceInvocationHandler handler = (AbstractServiceInvocationHandler)ProxyFactory.getInvocationHandler(service);
					ret = handler.getDomainService();
				}
				else
				{
					ret = service;
				}
			}
		}
		
		return ret;	
	}
	
	/**
	 *  Get the provided service info for a service.
	 *  @param sid The service identifier.
	 *  @return The provided service info.
	 */
	protected ProvidedServiceInfo getProvidedServiceInfo(IServiceIdentifier sid)
	{
		return serviceinfos.get(sid);
	}
	
	public Component getComponent()
	{
		return self;
	}
	
	/**
	 *  Guess a parameter.
	 *  @param type The type.
	 *  @param exact Test with exact 
	 *  @return The mapped value. 
	 * /
	public Object guessParameter(Class<?> type, boolean exact)
	{
		if(guesser==null)
			guesser	= new SimpleParameterGuesser(Collections.singleton(this));
		return guesser.guessParameter(type, exact);
	}*/
	
	/**
	 *  Add a method invocation handler.
	 */
	public void addMethodInvocationListener(IServiceIdentifier sid, MethodInfo mi, IMethodInvocationListener listener)
	{
//		System.out.println("added lis: "+sid+" "+mi+" "+hashCode());
		
		if(servicelisteners==null)
			servicelisteners = new HashMap<IServiceIdentifier, MethodListenerHandler>();
		MethodListenerHandler handler = servicelisteners.get(sid);
		if(handler==null)
		{
			handler = new MethodListenerHandler();
			servicelisteners.put(sid, handler);
		}
		handler.addMethodListener(mi, listener);
	}
	
	/**
	 *  Remove a method invocation handler.
	 */
	public void removeMethodInvocationListener(IServiceIdentifier sid, MethodInfo mi, IMethodInvocationListener listener)
	{
		if(servicelisteners!=null)
		{
			MethodListenerHandler handler = servicelisteners.get(sid);
			if(handler!=null)
			{
				handler.removeMethodListener(mi, listener);
			}
		}
	}
	
	/**
	 *  Notify listeners that a service method has been called.
	 */
	public void notifyMethodListeners(IServiceIdentifier sid, boolean start, Object proxy, final Method method, final Object[] args, Object callid, ServiceInvocationContext context)
	{
		if(servicelisteners!=null)
		{
			MethodListenerHandler handler = servicelisteners.get(sid);
			if(handler!=null)
			{
//				MethodInfo mi = new MethodInfo(method);
				handler.notifyMethodListeners(start, proxy, method, args, callid, context);
			}
		}
	}
	
	/**
	 *  Test if service and method has listeners.
	 */
	public boolean hasMethodListeners(IServiceIdentifier sid, MethodInfo mi)
	{
		boolean ret = false;
		if(servicelisteners!=null)
		{
			MethodListenerHandler handler = servicelisteners.get(sid);
			if(handler!=null)
			{
				ret = handler.hasMethodListeners(sid, mi);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Add a service proxy.
	 *  @param pojo The pojo.
	 *  @param proxy The proxy.
	 */
	public static void addPojoServiceProxy(Object pojo, IService proxy)
	{
//		System.out.println("add pojoproxy: "+proxy.getServiceIdentifier());
		
		synchronized(ProvidedServiceFeature.class)
		{
			if(pojoproxies==null)
				pojoproxies = new IdentityHashMap<Object, IService>();
			pojoproxies.put(pojo, proxy);
		}
	}
	
	/**
	 *  Remove a pojo - proxy pair.
	 *  @param sid The service identifier.
	 */
	public static void removePojoServiceProxy(IServiceIdentifier sid)
	{
		synchronized(ProvidedServiceFeature.class)
		{
			for(Iterator<IService> it=pojoproxies.values().iterator(); it.hasNext(); )
			{
				IService proxy = it.next();
				
				if(sid.equals(proxy.getServiceId()))
				{
					it.remove();
					break;
//					System.out.println("rem: "+pojosids.size());	
				}
			}
		}
	}
	
	/**
	 *  Get the proxy of a pojo service.
	 *  @param pojo The pojo service.
	 *  @return The proxy of the service.
	 */
	public static IService getPojoServiceProxy(Object pojo)
	{
		synchronized(ProvidedServiceFeature.class)
		{
			return pojoproxies.get(pojo);
		}
	}
	
	/**
	 *  Test if a service is a provided service proxy.
	 *  @param service The service.
	 *  @return True, if is provided service proxy.
	 */
	public static boolean isProvidedServiceProxy(Object service)
	{
		boolean ret = false;
		if(ProxyFactory.isProxyClass(service.getClass()))
		{
			Object tmp = ProxyFactory.getInvocationHandler(service);
			if(tmp instanceof AbstractServiceInvocationHandler)
			{
				AbstractServiceInvocationHandler handler = (AbstractServiceInvocationHandler)tmp;
				ret = !handler.isRequired();
			}
		}
		return ret;
	}
	
	public static Collection<String> evaluateTags(IComponent component, Collection<String> tags)
	{
		Collection<String> ret = new ArrayList<String>();
		IModelFeature mf = component.getFeature(IModelFeature.class);
		
		for(String tag: SUtil.notNull(tags))
		{
			try
			{
				// todo: hack, remove cast!
				Object ptag = SJavaParser.evaluateExpression(tag, mf.getModel().getAllImports(), component.getValueProvider().getFetcher(), ComponentManager.get().getClassLoader());
				ret.add(""+ptag);
			}
			catch(Exception e)
			{
				//e.printStackTrace();
				ret.add(tag);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Static method for creating a standard service proxy for a provided service.
	 */
	public IInternalService createProvidedServiceProxy(Component ia, Object service, 
		String name, Class<?> type, IServiceInvocationInterceptor[] ics, 
		//boolean monitoring, 
		ProvidedServiceInfo info)
	{
		IServiceIdentifier sid = null;
		
		if(isProvidedServiceProxy(service))
		{
			System.out.println("Already provided service proxy: "+service);
			return (IInternalService)service;
		}
		
		IInternalService ret;
		
		if(!SReflect.isSupertype(type, service.getClass()))
			throw new RuntimeException("Service implementation '"+service.getClass().getName()+"' does not implement service interface: "+type.getName());
		
		if(service instanceof IInternalService)
		{
			//sid = UUID.randomUUID();
			sid = BasicService.createServiceIdentifier(ia, name, type, service.getClass(), info, evaluateTags(self, info.getTags()));
			((IInternalService)service).setServiceIdentifier(sid);
		}
			
		
//		if(type.getName().indexOf("IServiceCallService")!=-1)
//			System.out.println("hijijij");
		String proxytype = info!=null && info.getImplementation()!=null && info.getImplementation().getProxytype()!=null
			? info.getImplementation().getProxytype() : AbstractServiceInvocationHandler.PROXYTYPE_DECOUPLED;
		
		if(!AbstractServiceInvocationHandler.PROXYTYPE_RAW.equals(proxytype) || (ics!=null && ics.length>0))
		{
			AbstractServiceInvocationHandler handler = createProvidedHandler(name, ia, type, service, info);
			if(sid==null)
			{
				Object ser = handler.getService();
				if(ser instanceof ServiceInfo)
					sid = ((ServiceInfo)ser).getManagementService().getServiceId();
			}
			ret	= (IInternalService)ProxyFactory.newProxyInstance(ia.getClassLoader(), new Class[]{IInternalService.class, type}, (InvocationHandler)handler);
//			try
//			{
//				((IService)service).getServiceIdentifier();
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
			
			addProvidedInterceptors(handler, service, ics, ia, proxytype, sid!=null? sid: ret.getServiceId());
//			ret	= (IInternalService)Proxy.newProxyInstance(ia.getExternalAccess()
//				.getModel().getClassLoader(), new Class[]{IInternalService.class, type}, handler);
			if(!(service instanceof IService))
			{
				if(!service.getClass().isAnnotationPresent(Service.class)
					// Hack!!! BPMN uses a proxy as service implementation.
					&& !(ProxyFactory.isProxyClass(service.getClass())
					&& ProxyFactory.getInvocationHandler(service).getClass().isAnnotationPresent(Service.class)))
				{
					//throw new RuntimeException("Pojo service must declare @Service annotation: "+service.getClass());
					//ia.getLogger().warning("Pojo service should declare @Service annotation: "+service.getClass());
//					throw new RuntimeException("Pojo service must declare @Service annotation: "+service.getClass());
					System.out.println("Pojo service should declare @Service annotation: "+service.getClass());
					//boolean b = ProxyFactory.isProxyClass(service.getClass());
					//System.out.println("isproxy: "+b);
				}
				addPojoServiceProxy(service, ret);
			}
		}
		else
		{
			if(service instanceof IInternalService)
			{
				ret	= (IInternalService)service;
			}
			else
			{
				throw new RuntimeException("Raw services must implement IInternalService (e.g. by extending BasicService): " + service.getClass().getCanonicalName());
			}
		}
		return ret;
	}
	
	/**
	 *  Add the standard and custom interceptors.
	 */
	public void addProvidedInterceptors(AbstractServiceInvocationHandler handler, Object service, 
		IServiceInvocationInterceptor[] ics, Component ia, String proxytype, 
		//boolean monitoring, 
		IServiceIdentifier sid)
	{
//		System.out.println("addI:"+service);

		// Only add standard interceptors if not raw.
		if(!AbstractServiceInvocationHandler.PROXYTYPE_RAW.equals(proxytype))
		{
			handler.addFirstServiceInterceptor(new MethodInvocationInterceptor());
			
			/*if(Starter.TRACING!=null && TracingMode.OFF!=Starter.TRACING)
			{
				//System.out.println("Tracing addProv: "+BasicServiceInvocationHandler.class.getClassLoader());
				handler.addFirstServiceInterceptor(new TracingInterceptor(ia));
			}*/
			
//			if(monitoring)
//				handler.addFirstServiceInterceptor(new MonitoringInterceptor(ia));
//			handler.addFirstServiceInterceptor(new AuthenticationInterceptor(ia, false));
			
			/*try
			{
				Class<?> clazz = sid.getServiceType().getType(ia.getClassLoader());
				boolean addhandler = false;
				Method[] ms = SReflect.getAllMethods(clazz);
				
				formethod:
				for (Method m : ms)
				{
					Annotation[] as = m.getAnnotations();
					for (Annotation anno : as)
						if (anno instanceof CheckNotNull 
							|| anno instanceof CheckState
							|| anno instanceof CheckIndex)
						{
							addhandler = true;
							break formethod;
						}
				}
				if (addhandler)
					handler.addFirstServiceInterceptor(new PrePostConditionInterceptor(ia));
			}
			catch (Exception e)
			{
			}*/
			
			if(!(service instanceof IService))
				handler.addFirstServiceInterceptor(new ResolveInterceptor(ia));
			
			handler.addFirstServiceInterceptor(new MethodCallListenerInterceptor(ia, sid));
//			handler.addFirstServiceInterceptor(new ValidationInterceptor(ia));
			if(!AbstractServiceInvocationHandler.PROXYTYPE_DIRECT.equals(proxytype))
				//handler.addFirstServiceInterceptor(new DecouplingInterceptor(ia, Starter.isParameterCopy(sid.getProviderId()), false));
				handler.addFirstServiceInterceptor(new DecouplingInterceptor(ia, true, false));
			handler.addFirstServiceInterceptor(new DecouplingReturnInterceptor());
			
			// used only by global service pool, todo add contionally
			//handler.addFirstServiceInterceptor(new IntelligentProxyInterceptor(ia.getExternalAccess(), sid));
		}
		
		if(ics!=null)
		{
			for(int i=0; i<ics.length; i++)
			{
				handler.addServiceInterceptor(ics[i], -1);
			}
		}
	}
	
	// very problematic method:
	// needs many annotations
	/**
	 *  Create a basic invocation handler for a provided service.
	 */
	protected static ServiceInvocationHandler createProvidedHandler(String name, Component ia, Class<?> type, Object service, ProvidedServiceInfo info)
	{
//		if(type.getName().indexOf("ITestService")!=-1 && ia.getComponentIdentifier().getName().startsWith("Global"))
//			System.out.println("gaga");
		
		/*Map<String, Object> serprops = new HashMap<String, Object>();
		if(info != null && info.getProperties() != null)
		{
			for(UnparsedExpression exp : info.getProperties())
			{
				Object val = SJavaParser.parseExpression(exp, ia.getFeature(IModelFeature.class).getModel().getAllImports(), ia.getClassLoader()).getValue(ia.getFeature(IModelFeature.class).getFetcher());
				serprops.put(exp.getName(), val);
			}
		}*/
		
		ServiceInvocationHandler handler;
		if(service instanceof IService)
		{
			IService ser = (IService)service;
			
			/*if(service instanceof BasicService)
			{
				//serprops.putAll(((BasicService)service).getPropertyMap());
				((BasicService)service).setPropertyMap(serprops);
			}*/
			
			handler = new ServiceInvocationHandler(ia, ser, false);
			
//			if(type==null)
//			{
//				type = ser.getServiceIdentifier().getServiceType();
//			}
//			else if(!type.equals(ser.getServiceIdentifier().getServiceType()))
//			{
//				throw new RuntimeException("Service does not match its type: "+type+", "+ser.getServiceIdentifier().getServiceType());
//			}
		}
		else
		{
			if(type==null)
			{
				// Try to find service interface via annotation
				if(service.getClass().isAnnotationPresent(Service.class))
				{
					Service si = (Service)service.getClass().getAnnotation(Service.class);
					if(!si.value().equals(Object.class))
					{
						type = si.value();
					}
				}
				// Otherwise take interface if there is only one
				else
				{
					Class<?>[] types = service.getClass().getInterfaces();
					if(types.length!=1)
						throw new RuntimeException("Unknown service interface: "+SUtil.arrayToString(types));
					type = types[0];
				}
			}
			
			Class<?> serclass = service.getClass();

			BasicService mgmntservice = new BasicService(ia.getId(), type, serclass, null);
			mgmntservice.setServiceIdentifier(BasicService.createServiceIdentifier(ia, name, type, service.getClass(), info, evaluateTags(ia, info.getTags())));
			//serprops.putAll(mgmntservice.getPropertyMap());
			//mgmntservice.setPropertyMap(serprops);
			
			// Do not try to call isAnnotationPresent for Proxy on Android
			// see http://code.google.com/p/android/issues/detail?id=24846
			if(!(ProxyFactory.isProxyClass(serclass)))
			//if(!(SReflect.isAndroid() && ProxyFactory.isProxyClass(serclass)))
			{
				while(!Object.class.equals(serclass))
				{
					Field[] fields = serclass.getDeclaredFields();
					for(int i=0; i<fields.length; i++)
					{
						if(fields[i].isAnnotationPresent(jadex.providedservice.annotation.ServiceIdentifier.class))
						{
							jadex.providedservice.annotation.ServiceIdentifier si = (jadex.providedservice.annotation.ServiceIdentifier)fields[i].getAnnotation(jadex.providedservice.annotation.ServiceIdentifier.class);
							if(si.value().equals(Object.class) || si.value().equals(type))
							{
								if(SReflect.isSupertype(IServiceIdentifier.class, fields[i].getType()))
								{
									try
									{
										SAccess.setAccessible(fields[i], true);
										fields[i].set(service, mgmntservice.getServiceId());
									}
									catch(Exception e)
									{
										e.printStackTrace();
									}
								}
								else
								{
									throw new RuntimeException("Field cannot store IServiceIdentifer: "+fields[i]);
								}
							}
						}
						
						if(fields[i].isAnnotationPresent(ServiceComponent.class))
						{
							try
							{
								Object val	= ia.getValueProvider().getParameterGuesser().guessParameter(fields[i].getType(), false);
								SAccess.setAccessible(fields[i], true);
								fields[i].set(service, val);
							}
							catch(Exception e)
							{
//								e.printStackTrace();
								throw new RuntimeException(e);
							}
						}
					}
					serclass = serclass.getSuperclass();
				}
			}
			
			ServiceInfo si = new ServiceInfo(service, mgmntservice);
			handler = new ServiceInvocationHandler(ia, si);//, ia.getDescription().getCause());
			
//			addPojoServiceIdentifier(service, mgmntservice.getServiceIdentifier());
		}
		
		return handler;
	}
}
