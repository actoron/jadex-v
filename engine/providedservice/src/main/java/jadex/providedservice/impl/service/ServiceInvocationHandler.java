package jadex.providedservice.impl.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jadex.bytecode.ProxyFactory;
import jadex.common.ClassInfo;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.UnparsedExpression;
import jadex.core.impl.Component;
import jadex.execution.future.FutureFunctionality;
import jadex.future.ExceptionDelegationResultListener;
import jadex.future.Future;
import jadex.future.FutureHelper;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;
import jadex.model.IModelFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.FutureReturnType;
import jadex.providedservice.annotation.Raw;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.ServiceComponent;
import jadex.providedservice.annotation.ServiceIdentifier;
import jadex.providedservice.impl.service.interceptors.DecouplingInterceptor;
import jadex.providedservice.impl.service.interceptors.DecouplingReturnInterceptor;
import jadex.providedservice.impl.service.interceptors.MethodInvocationInterceptor;
import jadex.providedservice.impl.service.interceptors.ResolveInterceptor;

/**
 *  Service invocation interceptor.
 *  It has a multi collection of interceptors per method.
 *  Executes the list of interceptors one by one.
 *  In case no handler can be found a fallback handler is used.
 */
public class ServiceInvocationHandler implements InvocationHandler, ISwitchCall
{
	//-------- constants --------
	
	/** The raw proxy type (i.e. no proxy). */
	public static final String	PROXYTYPE_RAW	= "raw";
	
	/** The direct proxy type (supports custom interceptors, but uses caller thread). */
	public static final String	PROXYTYPE_DIRECT	= "direct";
	
	/** The (default) decoupled proxy type (decouples from caller thread to component thread). */
	public static final String	PROXYTYPE_DECOUPLED	= "decoupled";
	
	//-------- attributes --------

	/** The internal access. */
	protected Component comp;
	
	// The proxy can be equipped with 
	// a) the IService Object
	// b) a service info object (for pojo services that separate basic service object and pojo service)
	// c) a service identifier that can be used to relay a call to another service
	
	/** The service identifier. */
	protected IServiceIdentifier sid;
	
	/** The service. */
	protected Object service;
		

	/** The logger for errors/warnings. */
	//protected Logger logger;

	/** The list of interceptors. */
	protected List<IServiceInvocationInterceptor> interceptors;
	
//	/** The root cause that was given at creation time. */
//	protected Cause cause;
	
//	/** The call id. */
//	protected AtomicLong callid;
	
	/** The flag if the proxy is required (provided otherwise). */
	protected boolean required;
	
	/** The flag if a switchcall should be done. */
	protected boolean switchcall;
	
	
	/** The pojo service map (pojo -> proxy). */
	protected static Map<Object, IService>	pojoproxies;

	
	//-------- constructors --------
	
	/**
	 *  Create a new invocation handler.
	 */
	public ServiceInvocationHandler(Component comp, IServiceIdentifier sid, 
		//Logger logger, 
		boolean required)
	{
//		assert cause!=null;
		this.comp = comp;
		this.sid = sid;
		//this.logger	= logger;
//		this.cause = cause;
		this.switchcall = true;
		this.required	= required;
//		this.callid = new AtomicLong();
	}
	
	/**
	 *  Create a new invocation handler.
	 */
	public ServiceInvocationHandler(Component comp, IService service, boolean required)
	{
//		assert cause!=null;
		this.comp = comp;
		this.service = service;
//		this.sid = service.getId();
		//this.logger	= logger;
//		this.realtime	= realtime;
//		this.cause = cause;
		this.switchcall = false; 
		this.required	= required;
//		this.callid = new AtomicLong();
	}
	
	/**
	 *  Create a new invocation handler.
	 */
	public ServiceInvocationHandler(Component comp, ServiceInfo service)
	{
//		assert cause!=null;
		this.comp = comp;
		this.service = service;
//		this.sid = service.getManagementService().getId();
		//this.logger	= logger;
//		this.realtime	= realtime;
//		this.cause = cause;
		this.switchcall = false; // called for provided proxy which must not switch (is the object that is asked in the req proxy)
//		this.callid = new AtomicLong();
	}
	
//	/**
//	 *  Create a new invocation handler.
//	 */
//	public BasicServiceInvocationHandler(IInternalAccess comp, IResultCommand<IFuture<Object>, Void> searchcmd, Logger logger, Cause cause)
//	{
//		assert cause!=null;
//		this.comp = comp;
//		this.searchcmd = searchcmd;
////		this.sid = service.getManagementService().getId();
//		this.logger	= logger;
////		this.realtime	= realtime;
//		this.cause = cause;
//		this.switchcall = false; // called for provided proxy which must not switch (is the object that is asked in the req proxy)
////		this.callid = new AtomicLong();
//	}
	
	//-------- methods --------
	
	/**
	 *  A proxy method has been invoked.
	 */
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
	{
		Object ret = null;
		
//		final long callid = this.callid.getAndIncrement();
//		comp.getServiceContainer().notifyMethodListeners(getServiceIdentifier(), true, proxy, method, args, callid, null);
		
//		if(method.getName().indexOf("start")!=-1 && getServiceIdentifier().getServiceType().getTypeName().indexOf("Peer")!=-1)
//			System.out.println("call method start");
//		if(method.getName().indexOf("updateClientData")!=-1 && args[0]==null)// && getServiceIdentifier().getServiceType().getTypeName().indexOf("Peer")!=-1)
//			System.out.println("call method init");
		
//		ServiceInvocationContext sicon = null;
		
		// IT IS IMPORTANT TO HANDLE getSericeId() HERE. Otherwise random bug behavior might occur
		if((args==null || args.length==0) && "getServiceId".equals(method.getName()))
		{
			ret	= getServiceIdentifier();
		}
		else if(args!=null && args.length==1 && args[0]!=null && "equals".equals(method.getName()) && Object.class.equals(method.getParameterTypes()[0]))
		{
			Object	cmp	= ProxyFactory.isProxyClass(args[0].getClass()) ? ProxyFactory.getInvocationHandler(args[0]) : args[0];
			ret	= equals(cmp);
		}
		else if(method.getAnnotation(Raw.class)!=null)
		{
			Object ser = null	;
			if(service instanceof IInternalService)
			{
				ser = service;
			}
			else if(service instanceof ServiceInfo)
			{
				ServiceInfo si = (ServiceInfo)service;
				if(ResolveInterceptor.SERVICEMETHODS.contains(method))
				{
					ser = si.getManagementService();
				}
				else
				{
					ser = si.getDomainService();
				}
			}
			/*else if(ProxyFactory.isProxyClass(service.getClass()) && 
				RemoteMethodInvocationHandler.class.equals(ProxyFactory.getInvocationHandler(service).getClass()))
			{
				ser = service;
			}
			else
			{
				throw new RuntimeException("Raw service cannot be invoked on: "+service);
			}*/
			
			if(ser==null)
				throw new RuntimeException("Raw service cannot be invoked on: "+service);
			
			ret = method.invoke(ser, args);
		}
		else if((args==null || args.length==0) && "hashCode".equals(method.getName()))
		{
//			System.out.println("hashcode on proxy: "+getServiceIdentifier().toString());
			ret	= hashCode();
		}
		else if((args==null || args.length==0) && "toString".equals(method.getName()))
		{
//			System.out.println("hashcode on proxy: "+getServiceIdentifier().toString());
			ret	= toString();
		}
		else
		{
			final ServiceInvocationContext sic = new ServiceInvocationContext(proxy, method, getInterceptors(), getServiceIdentifier());//, cause);
//			sicon = sic;
			
//			if(method.getName().indexOf("getExternalAccess")!=-1 && sic.getLastServiceCall()==null)
//				System.out.println("call method ex");
			
			List<Object> myargs = args!=null? SUtil.arrayToList(args): null;
			
			if(SReflect.isSupertype(IFuture.class, method.getReturnType()))
			{
				Class<?> rettype = null;
				Annotation[][] anss = method.getParameterAnnotations();
				for(int i=0; i<anss.length; i++)
				{
					Annotation[] ans = anss[i];
					for(Annotation an: ans)
					{
						if(an instanceof FutureReturnType)
						{
							Object t = myargs.get(i);
							if(t instanceof Class)
								rettype = (Class<?>)t;
							else if(t instanceof ClassInfo)
								rettype = ((ClassInfo)t).getType(comp.getClassLoader());
							if(rettype!=null)
								break;
						}
					}
				}
				/*if("invokeMethod".equals(method.getName()))
				{
					ClassInfo rtype = (ClassInfo)myargs.get(3);
					if(rtype!=null)
						rettype = rtype.getType(comp.getClassLoader());
				}*/
				
				if(rettype==null)
					rettype = method.getReturnType();
				
				@SuppressWarnings("unchecked")
				final Future<Object> fret = (Future<Object>)FutureFunctionality.getDelegationFuture(rettype, 
					new FutureFunctionality());
					//new FutureFunctionality(logger));
//					new ServiceCallFutureFunctionality(logger, sic.getLastServiceCall(), method.getName()));
				ret	= fret;
//				System.out.println("fret: "+fret+" "+method);
//				fret.addResultListener(new IResultListener()
//				{
//					public void resultAvailable(Object result)
//					{
//						System.out.println("fret res: "+result);
//					}
//					public void exceptionOccurred(Exception exception)
//					{
//						System.out.println("fret ex: "+exception);
//					}
//				});
//				if(method.getName().indexOf("addEntry")!=-1)
//					System.out.println("connect: ");
				sic.invoke(service, method, myargs).addResultListener(new ExceptionDelegationResultListener<Void, Object>(fret)
				{
					public void customResultAvailable(Void result)
					{
//						if(sic.getMethod().getName().indexOf("test")!=-1)
//							System.out.println("connect: "+sic.getMethod().getName());
//						if(method.getName().indexOf("start")!=-1 && getServiceIdentifier().getServiceType().getTypeName().indexOf("Peer")!=-1)
//							System.out.println("call method start end");
//						if(method.getName().indexOf("init")!=-1 && getServiceIdentifier().getServiceType().getTypeName().indexOf("Peer")!=-1)
//							System.out.println("call method init");
						try
						{
							// Although normally ret.getResult() is a future there are cases when not
							// because of mapping the method during the call (could be future method and inner one is not)
							if(sic.getResult() instanceof Exception)
							{
								fret.setException((Exception)sic.getResult());
							}
							else if(sic.getResult()!=null && !(sic.getResult() instanceof IFuture))
							{
								fret.setResult(sic.getResult());
							}
							else
							{
//								if(method.getName().equals("getRegisteredClients"))
//								{
//									System.err.println("connect getRegisteredClients future: "+fret+", "+sic.getResult());
//								}

								@SuppressWarnings("unchecked")
								IFuture<Object>	fut	= (IFuture<Object>)sic.getResult(); 
								fut.delegateTo(fret);
							}
						}
						catch(Exception e)
						{
							fret.setException(e);
						}
					}
				});
			}
//			else if(method.getReturnType().equals(void.class))
//			{
//				IFuture<Void> myvoid = sic.invoke(service, method, myargs);
//				
//				// Wait for the call to return to be able to throw exceptions
//				myvoid.get();
//				ret = sic.getResult();
//				
//				// Check result and propagate exception, if any.
//				// Do not throw exception as user code should not differentiate between local and remote case.
//	//			if(myvoid.isDone())
//	//			{
//	//				myvoid.get(null);	// throws exception, if any.
//	//			}
//	//			else
//				{
//					myvoid.addResultListener(new IResultListener<Void>()
//					{
//						public void resultAvailable(Void result)
//						{
//						}
//						
//						public void exceptionOccurred(Exception exception)
//						{
//							logger.warning("Exception in void method call: "+method+" "+getServiceIdentifier()+" "+exception);
//						}
//					});
//				}
//			}
			else
			{
//				if(method.getName().indexOf("Void")!=-1)
//					System.out.println("sdfdf");
				IFuture<Void> fut = sic.invoke(service, method, myargs);
				if(fut.isDone())
				{
					//fut.get();	
					ret = sic.getResult();
				}
				else
				{
					// Try again after triggering delayed notifications.
					FutureHelper.notifyStackedListeners();
					if(fut.isDone())
					{
//						System.out.println("stacked method: "+method);
						ret = sic.getResult();
					}
					else
					{
//						logger.warning("Warning, blocking call: "+method.getName()+" "+getServiceIdentifier());
						// Waiting for the call is ok because of component suspendable
						fut.get();
						ret = sic.getResult();
					}
				}
				if(ret instanceof Throwable)
					SUtil.rethrowAsUnchecked((Throwable)ret);
			}
		}
		
//		final ServiceInvocationContext fsicon = sicon;
//		if(ret instanceof IFuture)
//		{
//			((IFuture<Object>)ret).addResultListener(new IResultListener<Object>()
//			{
//				public void resultAvailable(Object result)
//				{
//					comp.getServiceContainer().notifyMethodListeners(getServiceIdentifier(), false, proxy, method, args, callid, fsicon);
//				}
//				
//				public void exceptionOccurred(Exception exception)
//				{
//					comp.getServiceContainer().notifyMethodListeners(getServiceIdentifier(), false, proxy, method, args, callid, fsicon);
//				}
//			});
//		}
//		else
//		{
//			comp.getServiceContainer().notifyMethodListeners(getServiceIdentifier(), false, proxy, method, args, callid, fsicon);
//		}
		
		return ret;
	}
	
	/**
	 *  Get the sid.
	 *  @return the sid.
	 */
	public IServiceIdentifier getServiceIdentifier()
	{
		if(sid==null)
		{
			// Hack!!! Preserve call context after getServiceIdentifier()
			ServiceCall	sc	= CallAccess.getNextInvocation();
			CallAccess.resetNextInvocation();
			
			sid = service instanceof ServiceInfo? ((ServiceInfo)service).getManagementService().getServiceId():
				((IService)service).getServiceId();
			
			CallAccess.setNextInvocation(sc);
		}
		return sid;
	}
	
	/**
	 *  Get the service.
	 *  @return The service.
	 */
	public Object getService()
	{
		return service;
	}
	
	/**
	 *  Get the domain service.
	 *  @return The domain service.
	 */
	public Object getDomainService()
	{
		return service instanceof ServiceInfo? ((ServiceInfo)service).getDomainService(): service;
	}

	/**
	 *  Add an interceptor.
	 *  
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized void addFirstServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		if(interceptors==null)
			interceptors = new ArrayList<IServiceInvocationInterceptor>();
		interceptors.add(0, interceptor);
	}
	
	/**
	 *  Add an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized void addServiceInterceptor(IServiceInvocationInterceptor interceptor, int pos)
	{
		if(interceptors==null)
			interceptors = new ArrayList<IServiceInvocationInterceptor>();
		// Hack? -1 for default position one before method invocation interceptor
		interceptors.add(pos>-1? pos: interceptors.size()-1, interceptor);
	}
	
	/**
	 *  Add an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized void addServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		addServiceInterceptor(interceptor, -1);
	}
	
	/**
	 *  Remove an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized void removeServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		if(interceptors!=null)
			interceptors.remove(interceptor);
	}
	
	/**
	 *  Get interceptors.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public synchronized IServiceInvocationInterceptor[] getInterceptors()
	{
		return interceptors==null || interceptors.size()==0? null://new IServiceInvocationInterceptor[]{fallback}: 
			(IServiceInvocationInterceptor[])interceptors.toArray(new IServiceInvocationInterceptor[interceptors.size()]);
	}
	
	//-------- replacement methods for service proxies --------
	
	/**
	 *  Return the hash code.
	 */
	public int hashCode()
	{
		return 31+getServiceIdentifier().hashCode();
	}
	
	/**
	 *  Test if two objects are equal.
	 */
	public boolean equals(Object obj)
	{
		return obj instanceof ServiceInvocationHandler && ((ServiceInvocationHandler)obj).getServiceIdentifier().equals(getServiceIdentifier());
	}
	
	/**
	 *  Get a string representation.
	 */
	public String toString()
	{
		return getServiceIdentifier().toString();
	}
	
	//-------- static methods --------
	
	/**
	 *  Static method for creating a standard service proxy for a provided service.
	 */
	public static IInternalService createProvidedServiceProxy(Component ia, Object service, 
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
			sid = BasicService.createServiceIdentifier(ia, name, type, service.getClass(), info);
			((IInternalService)service).setServiceIdentifier(sid);
		}
			
		
//		if(type.getName().indexOf("IServiceCallService")!=-1)
//			System.out.println("hijijij");
		String proxytype = info!=null && info.getImplementation()!=null && info.getImplementation().getProxytype()!=null
			? info.getImplementation().getProxytype() : ServiceInvocationHandler.PROXYTYPE_DECOUPLED;
		
		if(!PROXYTYPE_RAW.equals(proxytype) || (ics!=null && ics.length>0))
		{
			ServiceInvocationHandler handler = createProvidedHandler(name, ia, type, service, info);
			if(sid==null)
			{
				Object ser = handler.getService();
				if(ser instanceof ServiceInfo)
					sid = ((ServiceInfo)ser).getManagementService().getServiceId();
			}
			ret	= (IInternalService)ProxyFactory.newProxyInstance(ia.getClassLoader(), new Class[]{IInternalService.class, type}, handler);
//			try
//			{
//				((IService)service).getServiceIdentifier();
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
			
			ServiceInvocationHandler.addProvidedInterceptors(handler, service, ics, ia, proxytype, sid!=null? sid: ret.getServiceId());
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
					boolean b = ProxyFactory.isProxyClass(service.getClass());
					System.out.println(b);
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
	 *  Create a basic invocation handler for a provided service.
	 */
	protected static ServiceInvocationHandler createProvidedHandler(String name, Component ia, Class<?> type, Object service, ProvidedServiceInfo info)
	{
//		if(type.getName().indexOf("ITestService")!=-1 && ia.getComponentIdentifier().getName().startsWith("Global"))
//			System.out.println("gaga");
		
		Map<String, Object> serprops = new HashMap<String, Object>();
		if(info != null && info.getProperties() != null)
		{
			for(UnparsedExpression exp : info.getProperties())
			{
				Object val = SJavaParser.parseExpression(exp, ia.getFeature(IModelFeature.class).getModel().getAllImports(), ia.getClassLoader()).getValue(ia.getFeature(IModelFeature.class).getFetcher());
				serprops.put(exp.getName(), val);
			}
		}
		
		ServiceInvocationHandler handler;
		if(service instanceof IService)
		{
			IService ser = (IService)service;
			
			if(service instanceof BasicService)
			{
				//serprops.putAll(((BasicService)service).getPropertyMap());
				//((BasicService)service).setPropertyMap(serprops);
			}
			
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
			mgmntservice.setServiceIdentifier(BasicService.createServiceIdentifier(ia, name, type, service.getClass(), info));
			//mgmntservice.setServiceIdentifier(UUID.randomUUID());
			//serprops.putAll(mgmntservice.getPropertyMap());
			mgmntservice.setPropertyMap(serprops);
			
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
						if(fields[i].isAnnotationPresent(ServiceIdentifier.class))
						{
							ServiceIdentifier si = (ServiceIdentifier)fields[i].getAnnotation(ServiceIdentifier.class);
							if(si.value().equals(Object.class) || si.value().equals(type))
							{
								if(SReflect.isSupertype(UUID.class, fields[i].getType()))
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
								Object val	= ia.getFeature(IModelFeature.class).getParameterGuesser().guessParameter(fields[i].getType(), false);
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
	
	/**
	 *  Add the standard and custom interceptors.
	 */
	protected static void addProvidedInterceptors(ServiceInvocationHandler handler, Object service, 
		IServiceInvocationInterceptor[] ics, Component ia, String proxytype, 
		//boolean monitoring, 
		IServiceIdentifier sid)
	{
//		System.out.println("addI:"+service);

		// Only add standard interceptors if not raw.
		if(!PROXYTYPE_RAW.equals(proxytype))
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
			
			//handler.addFirstServiceInterceptor(new MethodCallListenerInterceptor(ia, sid));
//			handler.addFirstServiceInterceptor(new ValidationInterceptor(ia));
			if(!PROXYTYPE_DIRECT.equals(proxytype))
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
	
	/**
	 *  Static method for creating a delegation service proxy for 
	 *  provided service that is not offered by the component itself.
	 * /
	// TODO: not currently used in apps???
	public static IInternalService createDelegationProvidedServiceProxy(MjComponent ia, UUID sid, 
		RequiredServiceInfo info, RequiredServiceBinding binding, ClassLoader classloader, boolean realtime)
	{
		ServiceInvocationHandler handler = new ServiceInvocationHandler(ia, sid, false);//, ia.getDescription().getCause(), false);
		handler.addFirstServiceInterceptor(new MethodInvocationInterceptor());
//		handler.addFirstServiceInterceptor(new DelegationInterceptor(ia, info, binding, null, sid, realtime));	// TODO
		handler.addFirstServiceInterceptor(new DecouplingReturnInterceptor(/*ea, null,* /));
		return (IInternalService)ProxyFactory.newProxyInstance(classloader, new Class[]{IInternalService.class, info.getType().getType(classloader, ia.getModel().getAllImports())}, handler); //sid.getServiceType()
	}*/

	
	
	/**
	 *  Add a service proxy.
	 *  @param pojo The pojo.
	 *  @param proxy The proxy.
	 */
	public static void addPojoServiceProxy(Object pojo, IService proxy)
	{
//		System.out.println("add pojoproxy: "+proxy.getServiceIdentifier());
		
		synchronized(ServiceInvocationHandler.class)
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
		synchronized(ServiceInvocationHandler.class)
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
		synchronized(ServiceInvocationHandler.class)
		{
			return pojoproxies.get(pojo);
		}
	}
	
	/**
	 *  Check if a switch call should be done.
	 *  @return True, if switch should be done.
	 */
	public boolean isSwitchCall()
	{
		return switchcall;
	}
	
	/**
	 *  Check if the handler is for a required service proxy.
	 */
	public boolean isRequired()
	{
		return required;
	}
	
//	/**
//	 *  Add a method listener.
//	 */
//	public void addMethodListener(MethodInfo m, IMethodInvocationListener listener)
//	{
//		if(methodlisteners==null)
//			methodlisteners = new HashMap<MethodInfo, List<IMethodInvocationListener>>();
//		List<IMethodInvocationListener> lis = methodlisteners.get(m);
//		if(lis==null)
//		{
//			lis = new ArrayList<IMethodInvocationListener>();
//			methodlisteners.put(m, lis);
//		}
//		lis.add(listener);
//	}
//	
//	/**
//	 *  Add a method listener.
//	 */
//	public void removeMethodListener(MethodInfo m, IMethodInvocationListener listener)
//	{
//		if(methodlisteners!=null)
//		{
//			List<IMethodInvocationListener> lis = methodlisteners.get(m);
//			if(lis!=null)
//			{
//				lis.remove(listener);
//			}
//		}
//	}
//	
//	/**
//	 *  Notify registered listeners in case a method is called.
//	 */
//	protected void notifyMethodListeners(boolean start, Object proxy, final Method method, final Object[] args, long callid)
//	{
//		if(methodlisteners!=null)
//		{
//			doNotifyListeners(start, proxy, method, args, callid, methodlisteners.get(null));
//			doNotifyListeners(start, proxy, method, args, callid, methodlisteners.get((new MethodInfo(method))));
//		}
//	}
//	
//	/**
//	 *  Do notify the listeners.
//	 */
//	protected void doNotifyListeners(boolean start, Object proxy, final Method method, final Object[] args, long callid, List<IMethodInvocationListener> lis)
//	{
//		if(lis!=null)
//		{
//			for(IMethodInvocationListener ml: lis)
//			{
//				if(start)
//				{
//					ml.methodCallStarted(proxy, method, args, callid);
//				}
//				else
//				{
//					ml.methodCallFinished(proxy, method, args, callid);
//				}
//			}
//		}
//	}
	
//	/**
//	 * 
//	 */
//	public static void addPojoServiceIdentifier(Object pojo, IServiceIdentifier sid)
//	{
//		if(pojosids==null)
//		{
//			synchronized(BasicServiceInvocationHandler.class)
//			{
//				if(pojosids==null)
//				{
//					pojosids = Collections.synchronizedMap(new HashMap());
//				}
//			}
//		}
//		pojosids.put(pojo, sid);
////		System.out.println("add: "+pojosids.size());
//	}
//	
//	/**
//	 * 
//	 */
//	public static void removePojoServiceIdentifier(IServiceIdentifier sid)
//	{
//		if(pojosids!=null)
//		{
//			pojosids.values().remove(sid);
////			System.out.println("rem: "+pojosids.size());
//		}
//	}
//	
//	/**
//	 * 
//	 */
//	public static IServiceIdentifier getPojoServiceIdentifier(Object pojo)
//	{
//		return (IServiceIdentifier)pojosids.get(pojo);
//	}
	
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
			if(tmp instanceof ServiceInvocationHandler)
			{
				ServiceInvocationHandler handler = (ServiceInvocationHandler)tmp;
				ret = !handler.isRequired();
			}
		}
		return ret;
	}
}


