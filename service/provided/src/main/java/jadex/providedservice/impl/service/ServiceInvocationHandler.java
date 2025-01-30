package jadex.providedservice.impl.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import jadex.bytecode.ProxyFactory;
import jadex.common.ClassInfo;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.impl.Component;
import jadex.execution.FutureReturnType;
import jadex.execution.future.FutureFunctionality;
import jadex.future.ExceptionDelegationResultListener;
import jadex.future.Future;
import jadex.future.FutureHelper;
import jadex.future.IFuture;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Raw;
import jadex.providedservice.impl.service.interceptors.ResolveInterceptor;

/**
 *  Service invocation interceptor.
 *  It has a multi collection of interceptors per method.
 *  Executes the list of interceptors one by one.
 *  In case no handler can be found a fallback handler is used.
 */
public class ServiceInvocationHandler extends AbstractServiceInvocationHandler implements InvocationHandler, ISwitchCall
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
	
//	/** The root cause that was given at creation time. */
//	protected Cause cause;
	
//	/** The call id. */
//	protected AtomicLong callid;
	
	/** The flag if the proxy is required (provided otherwise). */
	protected boolean required;
	
	/** The flag if a switchcall should be done. */
	protected boolean switchcall;
	
	
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
			List<Object> myargs = args!=null? SUtil.arrayToList(args): null;
			
			if(SReflect.isSupertype(IFuture.class, method.getReturnType()))
			{
				final Future<Object> fret = FutureFunctionality.createReturnFuture(method, args, comp.getClassLoader());
				ret = fret;
				
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
	 * /
	public synchronized void addFirstServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		if(interceptors==null)
			interceptors = new ArrayList<IServiceInvocationInterceptor>();
		interceptors.add(0, interceptor);
	}*/
	
	/**
	 *  Add an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 * /
	public synchronized void addServiceInterceptor(IServiceInvocationInterceptor interceptor, int pos)
	{
		if(interceptors==null)
			interceptors = new ArrayList<IServiceInvocationInterceptor>();
		// Hack? -1 for default position one before method invocation interceptor
		interceptors.add(pos>-1? pos: interceptors.size()-1, interceptor);
	}*/
	
	/**
	 *  Add an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 * /
	public synchronized void addServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		addServiceInterceptor(interceptor, -1);
	}*/
	
	/**
	 *  Remove an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 * /
	public synchronized void removeServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		if(interceptors!=null)
			interceptors.remove(interceptor);
	}*/
	
	/**
	 *  Get interceptors.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 * /
	public synchronized IServiceInvocationInterceptor[] getInterceptors()
	{
		return interceptors==null || interceptors.size()==0? null://new IServiceInvocationInterceptor[]{fallback}: 
			(IServiceInvocationInterceptor[])interceptors.toArray(new IServiceInvocationInterceptor[interceptors.size()]);
	}*/
	
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
	
	

}


