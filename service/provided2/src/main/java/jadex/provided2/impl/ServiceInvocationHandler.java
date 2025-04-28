package jadex.provided2.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.IComponentManager;
import jadex.execution.future.FutureFunctionality;
import jadex.future.ExceptionDelegationResultListener;
import jadex.future.Future;
import jadex.future.FutureHelper;
import jadex.future.IFuture;
import jadex.provided2.IService;
import jadex.provided2.IServiceIdentifier;

/**
 *  Service invocation interceptor.
 *  It has a multi collection of interceptors per method.
 *  Executes the list of interceptors one by one.
 *  In case no handler can be found a fallback handler is used.
 */
public class ServiceInvocationHandler	implements InvocationHandler, ISwitchCall
{
	//-------- attributes --------

	/** The service identifier. */
	protected IServiceIdentifier sid;
	
	/** The service. */
	// a) the IService Object (required handler for provided proxy)
	// b) pojo service (provided handler)
	protected Object service;

	/** The flag if a switchcall should be done. */
	protected boolean switchcall;
	
	/** The list of interceptors. */
	public final List<IServiceInvocationInterceptor> interceptors = Collections.synchronizedList(new ArrayList<IServiceInvocationInterceptor>());
	
	//-------- constructors --------
	
	/**
	 *  Create a new invocation handler.
	 */
	public ServiceInvocationHandler(IServiceIdentifier sid, Object service)
	{
		this.sid = sid;
		this.service	= service;
		this.switchcall = true;
	}
	
	/**
	 *  Create a new invocation handler.
	 */
	public ServiceInvocationHandler(IService service)
	{
		this.service = service;
		this.sid = service.getServiceId();
		this.switchcall = false;
	}
	
	//-------- methods --------
	
	/**
	 *  A proxy method has been invoked.
	 */
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
	{
		Object ret = null;
		
		// IT IS IMPORTANT TO HANDLE getSericeId() HERE. Otherwise random bug behavior might occur
		if((args==null || args.length==0) && "getServiceId".equals(method.getName()))
		{
			ret	= getServiceIdentifier();
		}
		else if(args!=null && args.length==1 && args[0]!=null && "equals".equals(method.getName()) && Object.class.equals(method.getParameterTypes()[0]))
		{
			Object	cmp	= Proxy.isProxyClass(args[0].getClass()) ? Proxy.getInvocationHandler(args[0]) : args[0];
			ret	= equals(cmp);
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
				final Future<Object> fret = FutureFunctionality.createReturnFuture(method, args, IComponentManager.get().getClassLoader(), null);
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
	 *  Add an interceptor.
	 *  
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public void addFirstServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		//if(interceptors==null)
		//	interceptors = new ArrayList<IServiceInvocationInterceptor>();
		interceptors.add(0, interceptor);
	}
	
	/**
	 *  Add an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public void addServiceInterceptor(IServiceInvocationInterceptor interceptor, int pos)
	{
		//if(interceptors==null)
		//	interceptors = new ArrayList<IServiceInvocationInterceptor>();
		// Hack? -1 for default position one before method invocation interceptor
		interceptors.add(pos>-1? pos: interceptors.size()-1, interceptor);
	}
	
	/**
	 *  Add an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public void addServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		addServiceInterceptor(interceptor, -1);
	}
	
	/**
	 *  Remove an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public void removeServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		//if(interceptors!=null)
		interceptors.remove(interceptor);
	}
	
	/**
	 *  Get interceptors.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public IServiceInvocationInterceptor[] getInterceptors()
	{
		IServiceInvocationInterceptor[] ret = interceptors==null || interceptors.size()==0? null://new IServiceInvocationInterceptor[]{fallback}: 
			(IServiceInvocationInterceptor[])interceptors.toArray(new IServiceInvocationInterceptor[interceptors.size()]);
	
		//System.out.println("intercepts: "+this+" "+ret.length+" "+interceptors.hashCode());
		
		return ret;
	}
	
	/**
	 *  Get the sid.
	 *  @return the sid.
	 */
	public IServiceIdentifier getServiceIdentifier()
	{
		return sid;
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
	
	/**
	 *  Check if a switch call should be done.
	 *  @return True, if switch should be done.
	 */
	public boolean isSwitchCall()
	{
		return switchcall;
	}
}


