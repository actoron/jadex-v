package jadex.mj.feature.providedservice.impl.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

import jadex.common.IParameterGuesser;
import jadex.common.IValueFetcher;
import jadex.common.UnparsedExpression;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;
import jadex.mj.core.MjComponent;
import jadex.mj.core.modelinfo.ModelInfo;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.lifecycle.impl.IMjLifecycle;
import jadex.mj.feature.providedservice.impl.service.IMjProvidedServiceFeature;
import jadex.mj.feature.providedservice.impl.service.ServiceScope;
import jadex.mj.micro.MjMicroAgent;
import jadex.serialization.ISerializationServices;
import jadex.serialization.SerializationServices;

public class MjProvidedServiceFeature	implements IMjLifecycle, IMjProvidedServiceFeature//, IParameterGuesser
{
	protected MjComponent self;
	
	protected IParameterGuesser	guesser;
	
	/** The map of platform services. */
	protected Map<Class<?>, Collection<IInternalService>> services;
	
	/** The map of provided service infos. (sid -> provided service info) */
	protected Map<UUID, ProvidedServiceInfo> serviceinfos;
	
	protected MjProvidedServiceFeature(MjComponent self)
	{
		this.self	= self;
	}
	
	@Override
	public IFuture<Void> onStart()
	{
		Future<Void> ret = new Future();
		
		ModelInfo model = self.getModel();
		
		Object mymodel = model.getFeatureModel(this.getClass());
		if(mymodel==null)
		{
			Object fmodel = ProvidedServiceLoader.readFeatureModel(((MjMicroAgent)self).getPojo().getClass(), this.getClass().getClassLoader());
			model.putFeatureModel(IMjProvidedServiceFeature.class, fmodel);
			
			// todo: save model to cache
		}
		
		// Collect provided services from model (name or type -> provided service info)
		ProvidedServiceInfo[] ps = (ProvidedServiceInfo[])model.getFeatureModel(IMjProvidedServiceFeature.class);
		Map<Object, ProvidedServiceInfo> sermap = new LinkedHashMap<Object, ProvidedServiceInfo>();
		for(int i=0; i<ps.length; i++)
		{
			Object key = ps[i].getName()!=null? ps[i].getName(): ps[i].getType().getType(self.getClass().getClassLoader(), model.getAllImports());
			if(sermap.put(key, ps[i])!=null)
			{
				ret.setException(new RuntimeException("Services with same type must have different name."));  // Is catched and set to ret below
				return ret;
			}
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
			ServiceScope scope = info.getScope();
			if(ServiceScope.EXPRESSION.equals(scope))
			{
				scope = (ServiceScope)SJavaParser.getParsedValue(info.getScopeExpression(), model.getAllImports(), self.getFetcher(), self.getClassLoader());
				info = new ProvidedServiceInfo(info.getName(), info.getType(), info.getImplementation(), scope, info.getScopeExpression(), info.getSecurity(), info.getPublish(), info.getProperties(), info.isSystemService());
//						System.out.println("expression scope '"
//							+ (info.getScopeExpression()!=null ? info.getScopeExpression().getValue() : "")
//							+ "': "+scope);
			}
				
			final Future<Void> fut = new Future<>();
			bar.addFuture(fut);
			
			final ProvidedServiceImplementation	impl = info.getImplementation();
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
				createServiceImplementation(info, self.getFetcher())
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
									ics[i] = (IServiceInvocationInterceptor)SJavaParser.evaluateExpression(ins[i].getValue(), self.getModel().getAllImports(), self.getFetcher(), self.getClassLoader());
								}
								else
								{
									try
									{
										ics[i] = (IServiceInvocationInterceptor)ins[i].getClazz().getType(self.getClassLoader(), self.getModel().getAllImports()).newInstance();
									}
									catch(Exception e)
									{
										e.printStackTrace();
									}
								}
							}
						}
						
						final Class<?> type = finfo.getType().getType(self.getClassLoader(), self.getModel().getAllImports());
						//PublishEventLevel elm = component.getDescription().getMonitoring()!=null? component.getDescription().getMonitoring(): null;
//								 todo: remove this? currently the level cannot be turned on due to missing interceptor
						//boolean moni = elm!=null? !PublishEventLevel.OFF.equals(elm.getLevel()): false; 
						final IInternalService proxy = ServiceInvocationHandler.createProvidedServiceProxy(
							self, ser, finfo.getName(), type, ics,
							//moni, 
							finfo);
						
						addService(proxy, finfo);
					}
					fut.setResult(null);
					
				}).catchEx(e -> {e.printStackTrace(); fut.setResult(null);});
			//}
		}
		
		bar.waitFor().then(v ->
		{
			// Start the services.
			Collection<IInternalService> allservices = getAllServices();
			if(!allservices.isEmpty())
			{
				initServices(allservices.iterator()).addResultListener(new DelegationResultListener<Void>(ret));
			}
			else
			{
				ret.setResult(null);
			}
		}).catchEx(ret);
		
		return ret;
	}
	
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
				ser = SJavaParser.getParsedValue(impl, self.getModel().getAllImports(), fetcher, self.getClassLoader());
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
			if(impl.getClazz().getType(self.getClassLoader(), self.getModel().getAllImports())!=null)
			{
				try
				{
					ser = impl.getClazz().getType(self.getClassLoader(), self.getModel().getAllImports()).newInstance();
					ret.setResult(ser);
				}
				catch(Exception e)
				{
					ret.setException(e);
				}
			}
			else
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
			}
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
			serviceinfos = new HashMap<UUID, ProvidedServiceInfo>();
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
		Class<?> servicetype = service.getClass();
				
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
		
		
		// todo !!!
		//ServiceRegistry.getRegistry(self.getId()).addLocalService(service);
		
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
		System.out.println("Starting service: "+is.getServiceId()+" "+self.getFeature(IMjExecutionFeature.class).isComponentThread());
		is.setComponentAccess(self).addResultListener(new DelegationResultListener<Void>(ret)
		{
			public void customResultAvailable(Void result)
			{
//				System.out.println("Starting service: "+is.getServiceId()+" "+component.getFeature(IExecutionFeature.class).isComponentThread());
				is.startService().addResultListener(new DelegationResultListener<Void>(ret)
				{
					public void customResultAvailable(Void result)
					{
						System.out.println("Started service: "+is.getServiceId());
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
		PublishInfo pit = info==null? null: info.getPublish();
		
		if(pit!=null)
		{
			// Hack?! evaluate the publish id string 
			// Must clone info to not change the model
			final PublishInfo pi = new PublishInfo(pit);
			try
			{
				String pid = (String)SJavaParser.evaluateExpression(pi.getPublishId(), getComponent().getModel().getAllImports(), getComponent().getFetcher(), getComponent().getClassLoader());
				pi.setPublishId(pid);
				System.out.println("pid is now: "+pid);
			}
			catch(Exception e)
			{
//				e.printStackTrace();
			}
			
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
		else
		{
			ret.setResult(null);
		}
		return ret;
	}
	
	/**
	 *  Get the provided service info for a service.
	 *  @param sid The service identifier.
	 *  @return The provided service info.
	 */
	protected ProvidedServiceInfo getProvidedServiceInfo(UUID sid)
	{
		return serviceinfos.get(sid);
	}
	
	public MjComponent getComponent()
	{
		return self;
	}
	
	protected ISerializationServices serser;
	
	public ISerializationServices getSerializationService()
	{
		if(serser==null)
			serser = new SerializationServices(getComponent().getId());
		return serser;
	}
	
	@Override
	public IFuture<Void> onBody()
	{
		return IFuture.DONE;
	}
	
	@Override
	public IFuture<Void> onEnd()
	{
		return IFuture.DONE;
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
}
