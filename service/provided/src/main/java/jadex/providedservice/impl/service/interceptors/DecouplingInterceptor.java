package jadex.providedservice.impl.service.interceptors;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import jadex.collection.LRU;
import jadex.common.ICommand;
import jadex.common.IFilter;
import jadex.common.SUtil;
import jadex.common.TimeoutException;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.Component;
import jadex.execution.IExecutionFeature;
import jadex.execution.future.FutureFunctionality;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.impl.service.IServiceInvocationInterceptor;
import jadex.providedservice.impl.service.ServiceInvocationContext;

/**
 *  Invocation interceptor for executing a call on 
 *  the underlying component thread. 
 *  
 *  It checks whether the call can be decoupled (has void or IFuture return type)
 *  and the invoking thread is not already the component thread.
 *  
 *  todo: what about synchronous calls that change the object state.
 *  These calls could damage the service state.
 */
public class DecouplingInterceptor extends AbstractMultiInterceptor
{
	//-------- constants --------
	
	/** The static map of subinterceptors (method -> interceptor). */
	protected static final Map<Method, IServiceInvocationInterceptor> SUBINTERCEPTORS = getInterceptors();

	/** The reference method cache (method -> boolean[] (is reference)). */
	public static final Map methodreferences = Collections.synchronizedMap(new LRU(500));

	//-------- attributes --------
	
	/** The internal access. */
	protected IComponent ia;	
		
	/** The clone filter (facade for marshal). */
	protected IFilter filter;
	
	//-------- constructors --------
	
	/**
	 *  Create a new invocation handler.
	 */
	public DecouplingInterceptor(IComponent ia)
	{
		this.ia = ia;
	}
	
	//-------- methods --------
	
	/**
	 *  Execute the command.
	 *  @param args The argument(s) for the call.
	 *  @return The result of the command.
	 */
	public IFuture<Void> doExecute(final ServiceInvocationContext sic)
	{
		final Future<Void> ret = new Future<Void>();
		
		// todo:
		/*if(required)
		{
			UUID caller	= IComponentIdentifier.LOCAL.get();
			if(caller!=null && !caller.equals(ea.getId()))
				throw new RuntimeException("Cannot invoke required service of other component '"+ea.getId()+"' from component '"+caller+"'. Service method: "+sic.getMethod());
		}*/
		
		// Perform argument copy
		
		// In case of remote call parameters are copied as part of marshalling.
		if(!sic.isRemoteCall())
		{
//			if(sic.getMethod().getName().indexOf("Stream")!=-1)
//				System.out.println("sdfsdfsdf");
			
			Method method = sic.getMethod();
			AnnotatedType[]	params	= method.getAnnotatedParameterTypes();
//			boolean[] refs = getReferenceInfo(method, !copy, true);
			
			Object[] args = sic.getArgumentArray();
			List<Object> copyargs = new ArrayList<Object>(); 
			if(args.length>0)
			{
				for(int i=0; i<args.length; i++)
				{
					copyargs.add(Component.copyVal(args[i], params[i].getAnnotations()));
				}
//				System.out.println("call: "+method.getName()+" "+notcopied+" "+SUtil.arrayToString(method.getParameterTypes()));//+" "+SUtil.arrayToString(args));
				sic.setArguments(copyargs);
			}
		}
		
		// Perform pojo service replacement (for local and remote calls).
		// Now done in RemoteServiceManagementService in XMLWriter
//		List args = sic.getArguments();
//		if(args!=null)
//		{
//			for(int i=0; i<args.size(); i++)
//			{
//				// Test if it is pojo service impl.
//				// Has to be mapped to new proxy then
//				Object arg = args.get(i);
//				if(arg!=null && !(arg instanceof BasicService) && arg.getClass().isAnnotationPresent(Service.class))
//				{
//					// Check if the argument type refers to the pojo service
//					Service ser = arg.getClass().getAnnotation(Service.class);
//					if(SReflect.isSupertype(ser.value(), sic.getMethod().getParameterTypes()[i]))
//					{
//						Object proxy = BasicServiceInvocationHandler.getPojoServiceProxy(arg);
////						System.out.println("proxy: "+proxy);
//						args.set(i, proxy);
//					}
//				}
//			}
//		}
		
		// Perform decoupling
		
		
		boolean scheduleable = true;
//		boolean scheduleable = SReflect.isSupertype(IFuture.class, sic.getMethod().getReturnType())
//			|| sic.getMethod().getReturnType().equals(void.class);
		
//		boolean scheduleable = sic.getMethod().getReturnType().equals(IFuture.class) 
//			|| sic.getMethod().getReturnType().equals(void.class);

//		if(sic.getMethod().getName().indexOf("getChildren")!=-1)
//			System.out.println("huhuhu");
		
		if(ia.equals(IComponentManager.get().getCurrentComponent()) || !scheduleable)
		{
			// Not possible to use if it complains this way
			// E.g. you have prov service and need to reschedule on the component then first getProviderId(), getExtAccess(), scheduleStep
//			if(!scheduleable && adapter.isExternalThread())
//				throw new RuntimeException("Must be called on component thread: "+Thread.currentThread()+" "+sic.getMethod().getName());
			
//			if(sic.getMethod().getName().equals("add"))
//				System.out.println("direct: "+Thread.currentThread());
//			sic.invoke().addResultListener(new TimeoutResultListener<Void>(10000, ea, 
//				new CopyReturnValueResultListener(ret, sic)));
			sic.invoke().addResultListener(new CopyReturnValueResultListener(ret, sic));
		}
		else
		{
//			if(sic.getMethod().getName().indexOf("getExternalAccess")!=-1
//				&& sic.getArguments().size()>0
//				&& sic.getArguments().get(0) instanceof IComponentIdentifier)
//			{
//				if(sic.getObject() instanceof ServiceInfo)
//				{
//					IComponentIdentifier	provider	= ((ServiceInfo)sic.getObject()).getManagementService().getId().getProviderId();
//					System.out.println("getExternalAccess: "+provider+", "+sic.getArguments());
//				}
//			}				
			
			// todo: why immediate? keep services responsive during suspend?
			/*ea.scheduleStep(IExecutionFeature.STEP_PRIORITY_IMMEDIATE, false, new InvokeMethodStep(sic))
				.addResultListener(new CopyReturnValueResultListener(ret, sic));*/
			
			// todo :
			
			//ia.getFeature(IMjExecutionFeature.class).scheduleStep(IMjExecutionFeature.STEP_PRIORITY_UNSET, false, new InvokeMethodStep(sic))
			//	.addResultListener(new CopyReturnValueResultListener(ret, sic));
			
			ia.getFeature(IExecutionFeature.class).scheduleAsyncStep(new InvokeMethodStep(sic))
				.addResultListener(new CopyReturnValueResultListener(ret, sic));

		}
		
		return ret;
	}
	
	/**
	 *  Get a sub interceptor for special cases.
	 *  @param sic The context.
	 *  @return The interceptor (if any).
	 */
	public IServiceInvocationInterceptor getInterceptor(ServiceInvocationContext sic)
	{
		return (IServiceInvocationInterceptor)SUBINTERCEPTORS.get(sic.getMethod());
	}
	
	/**
	 *  Get the sub interceptors for special cases.
	 */
	public static Map<Method, IServiceInvocationInterceptor> getInterceptors()
	{
		Map<Method, IServiceInvocationInterceptor> ret = new HashMap<Method, IServiceInvocationInterceptor>();
		try
		{
			ret.put(Object.class.getMethod("toString", new Class[0]), new AbstractApplicableInterceptor()
			{
				public IFuture<Void> execute(ServiceInvocationContext context)
				{
					Object proxy = context.getProxy();
					InvocationHandler handler = (InvocationHandler)Proxy.getInvocationHandler(proxy);
					context.setResult(handler.toString());
					return IFuture.DONE;
				}
			});
			ret.put(Object.class.getMethod("equals", new Class[]{Object.class}), new AbstractApplicableInterceptor()
			{
				public IFuture<Void> execute(ServiceInvocationContext context)
				{
					Object proxy = context.getProxy();
					InvocationHandler handler = (InvocationHandler)Proxy.getInvocationHandler(proxy);
					Object[] args = (Object[])context.getArguments().toArray();
					context.setResult(Boolean.valueOf(args[0]!=null && Proxy.isProxyClass(args[0].getClass())
						&& handler.equals(Proxy.getInvocationHandler(args[0]))));
					return IFuture.DONE;
				}
			});
			ret.put(Object.class.getMethod("hashCode", new Class[0]), new AbstractApplicableInterceptor()
			{
				public IFuture<Void> execute(ServiceInvocationContext context)
				{
					Object proxy = context.getProxy();
					InvocationHandler handler = Proxy.getInvocationHandler(proxy);
					context.setResult(Integer.valueOf(handler.hashCode()));
					return IFuture.DONE;
				}
			});
			// todo: other object methods?!
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}	
	
	//-------- helper classes --------
	
	/**
	 *  Copy return value, when service call is finished.
	 */
	protected class CopyReturnValueResultListener extends DelegationResultListener<Void>
	{
		//-------- attributes --------
		
		/** The service invocation context. */
		protected ServiceInvocationContext	sic;
		
		//-------- constructors --------
		
		/**
		 *  Create a result listener.
		 */
		protected CopyReturnValueResultListener(Future<Void> future, ServiceInvocationContext sic)
		{
			super(future);
			this.sic = sic;
		}
		
		//-------- IResultListener interface --------

		/**
		 *  Called when the service call is finished.
		 */
		public void customResultAvailable(Void result)
		{
			final Object	res	= sic.getResult();
			
//			if(sic.getMethod().getName().equals("getInputStream"))
//				System.out.println("decoupling: "+sic.getArguments());
			
			if(res instanceof IFuture)
			{
				Method method = sic.getMethod();

				// For local call: fetch timeout to decide if undone. ignored for remote.
				//final long timeout = !sic.isRemoteCall() ? sic.getNextServiceCall().getTimeout() : Timeout.NONE;

				FutureFunctionality func = new FutureFunctionality()//ia.getLogger())
				{
					TimeoutException ex = null;
					
					@Override
					public Object handleIntermediateResult(Object result) throws Exception
					{
//						//-------- debugging --------
//						if((""+result).contains("PartDataChunk"))
//						{
//							Logger.getLogger(getClass().getName()).info("handleIntermediateResult: "+sic+", "+result+", "+IComponentIdentifier.LOCAL.get());
//						}
//						//-------- debugging end --------
//						if(method.getName().equals("getRegisteredClients"))
//						{
//							System.err.println("Copy return value handleIntermediateResult of getRegisteredClients call: "+res+", "+result+", "+IComponentIdentifier.LOCAL.get());
//							Thread.dumpStack();
//						}
						
						if(ex!=null)
							throw ex;
						return sic.isRemoteCall() ? result : Component.copyVal(result, method.getAnnotatedReturnType().getAnnotations());
					}
					
					@Override
					public void handleFinished(Collection<Object> results) throws Exception
					{
						if(ex!=null)
							throw ex;
					}

					@Override
					public Object handleResult(Object result) throws Exception
					{
						if(ex!=null)
							throw ex;
						return sic.isRemoteCall() ? result : Component.copyVal(result, method.getAnnotatedReturnType().getAnnotations());
					}
					
					@Override
					public boolean isUndone(boolean undone)
					{
						// Always undone when (potentially) timeout exception.
						return undone;// || timeout>=0;
					}
					
//					public synchronized Exception setException(Exception exception)
//					{
//						if(ex!=null)
//							throw ex;
//						if(exception instanceof TimeoutException)
//							ex = (TimeoutException)exception;
//						return exception;
//					}
//					
//					public synchronized Exception setExceptionIfUndone(Exception exception)
//					{
//						if(ex!=null)
//							throw ex;
//						if(exception instanceof TimeoutException)
//							ex = (TimeoutException)exception;
//						return exception;
//					}
					
					@Override
					public void scheduleBackward(final ICommand<Void> code)
					{
						if(ia.getFeature(IExecutionFeature.class).isComponentThread())
						{
							code.execute(null);
						}
						else
						{
							/*ea.scheduleStep(new IComponentStep<Void>()
							{
								public IFuture<Void> execute(IInternalAccess ia)
								{
									code.execute(null);
									return IFuture.DONE;
								}
							});*/
							
							ia.getFeature(IExecutionFeature.class).scheduleStep(() -> code.execute(null));
						}	
					}
				};
				
//				String resstring	= sic.getMethod().getName().equals("getRegisteredClients") ? res.toString() : null;	// string before connect to see storeforfirst results
				
				final Future<?> fut = FutureFunctionality.getDelegationFuture((IFuture<?>)res, func);

//				if(method.getName().equals("getRegisteredClients"))
//				{
//					System.err.println("Copy return value getDelegationFuture of getRegisteredClients call: "+resstring+", "+fut+", "+IComponentIdentifier.LOCAL.get());
//					Thread.dumpStack();
//				}
				
				// Add timeout handling for local case.
				/*if(!((IFuture<?>)res).isDone() && !sic.isRemoteCall())
				{
//					boolean	realtime = sic.getNextServiceCall().getRealtime();
					
					if(timeout>=0)
					{
						if(fut instanceof IIntermediateFuture)
						{
//							TimeoutIntermediateResultListener	tirl	= new TimeoutIntermediateResultListener(timeout, ea, realtime, sic.getMethod(), new IIntermediateFutureCommandResultListener()
							TimeoutIntermediateResultListener	tirl	= new TimeoutIntermediateResultListener(timeout, ea, false, sic.getMethod(), new IIntermediateFutureCommandResultListener()
							{
								public void resultAvailable(Object result)
								{
									// Ignore if result is normally set.
								}
								/*public void resultAvailable(Collection result)
								{
									// Ignore if result is normally set.
								}* /
								public void exceptionOccurred(Exception exception)
								{
									// Forward timeout exception to future.
									if(exception instanceof TimeoutException)
									{
										fut.setExceptionIfUndone(exception);
										if(res instanceof ITerminableFuture<?>)
										{
											((ITerminableFuture)fut).terminate(exception);
										}
									}
								}
								public void intermediateResultAvailable(Object result)
								{
								}
								public void finished()
								{
								}
								public void commandAvailable(Object command)
								{
								}
								public void maxResultCountAvailable(int max) 
								{
								}
							});
							if(fut instanceof ISubscriptionIntermediateFuture)
							{
								((ISubscriptionIntermediateFuture)fut).addQuietListener(tirl);
							}
							else
							{
								fut.addResultListener(tirl);
							}
						}
						else
						{
//							SIC.set(sic);
//							fut.addResultListener(new TimeoutResultListener(timeout, ea, realtime, sic.getMethod(), new IFutureCommandResultListener()
							fut.addResultListener(new TimeoutResultListener(timeout, ea, false, sic.getMethod()+", "+sic.getArguments(), new IFutureCommandResultListener()
							{
								public void resultAvailable(Object result)
								{
									// Ignore if result is normally set.
								}
								
								public void exceptionOccurred(Exception exception)
								{
									// Forward timeout exception to future.
									if(exception instanceof TimeoutException)
									{
										fut.setExceptionIfUndone(exception);
										if(fut instanceof ITerminableFuture<?>)
										{
											((ITerminableFuture)fut).terminate(exception);
										}
									}
								}
								
								public void commandAvailable(Object command)
								{
//									if(fut instanceof ICommandFuture)
//									{
//										((ICommandFuture)fut).sendCommand(command);
//									}
//									else
//									{
//										System.out.println("Cannot forward command: "+fut+" "+command);
//									}
								}
							}));
						}
					}
				}*/
				
				sic.setResult(fut);
			}
			super.customResultAvailable(null);
		}
	}

//	public static ThreadLocal<ServiceInvocationContext>	SIC	= new ThreadLocal<ServiceInvocationContext>();
	
	/**
	 *  Service invocation step.
	 * /
	// Not anonymous class to avoid dependency to XML required for XMLClassname
	public static class InvokeMethodStep implements IComponentStep<Void>
	{
		// For debugging simulation blocker heisenbug -> TODO: remove when fixed
		protected static final Map<ServiceInvocationContext, String>	_DEBUG	= Collections.synchronizedMap(new WeakHashMap<>());
		public static final ThreadLocal<String>	DEBUG	= new ThreadLocal<>();
		
		protected ServiceInvocationContext sic;

		/**
		 *  Create an invoke method step.
		 * /
		public InvokeMethodStep(ServiceInvocationContext sic)
		{
			this.sic = sic;
			
			if(sic.getMethod().getName().equals("addAdvanceBlocker"))
			{
				Exception 	e	= new RuntimeException("addAdvanceBlocker called");
				e.fillInStackTrace();
				_DEBUG.put(sic, SUtil.getExceptionStacktrace(e));
			}
		}

		/**
		 *  Execute the step.
		 * /
		public IFuture<Void> execute(IInternalAccess ia)
		{					
			IFuture<Void> ret;
			
//			CallAccess.setServiceCall(sic.getServiceCall());
			
			try
			{
//				sic.setObject(service);
				DEBUG.set(_DEBUG.get(sic));
				ret	= sic.invoke();
			}
			catch(Exception e)
			{
//				e.printStackTrace();
				ret	= new Future<Void>(e);
			}
			finally
			{
				DEBUG.remove();
			}
			
//			if(sic.getLastServiceCall()==null)
//			{
//				CallAccess.resetServiceCall();
//			}
//			else
//			{
//				CallAccess.setServiceCall(sic.getLastServiceCall());
//			}
			return ret;
		}

		public String toString()
		{
			return "invokeMethod("+sic.getMethod()+", "+sic.getArguments()+")";
		}
	}*/
	
	/**
	 *  Service invocation step.
	 */
	// Not anonymous class to avoid dependency to XML required for XMLClassname
	public static class InvokeMethodStep implements Callable //Callable<Future<? extends Object>>
	{
		// For debugging simulation blocker heisenbug -> TODO: remove when fixed
		protected static final Map<ServiceInvocationContext, String>	_DEBUG	= Collections.synchronizedMap(new WeakHashMap<>());
		public static final ThreadLocal<String>	DEBUG	= new ThreadLocal<>();
		
		protected ServiceInvocationContext sic;

		/**
		 *  Create an invoke method step.
		 */
		public InvokeMethodStep(ServiceInvocationContext sic)
		{
			this.sic = sic;
			
			if(sic.getMethod().getName().equals("addAdvanceBlocker"))
			{
				Exception 	e	= new RuntimeException("addAdvanceBlocker called");
				e.fillInStackTrace();
				_DEBUG.put(sic, SUtil.getExceptionStacktrace(e));
			}
		}

		/**
		 *  Execute the step.
		 */
		public IFuture<Void> call()
		{					
			IFuture<Void> ret;
			
//			CallAccess.setServiceCall(sic.getServiceCall());
			
			try
			{
//				sic.setObject(service);
				DEBUG.set(_DEBUG.get(sic));
				ret	= sic.invoke();
			}
			catch(Exception e)
			{
//				e.printStackTrace();
				ret	= new Future<Void>(e);
			}
			finally
			{
				DEBUG.remove();
			}
			
//			if(sic.getLastServiceCall()==null)
//			{
//				CallAccess.resetServiceCall();
//			}
//			else
//			{
//				CallAccess.setServiceCall(sic.getLastServiceCall());
//			}
			return ret;
		}

		public String toString()
		{
			return "invokeMethod("+sic.getMethod()+", "+sic.getArguments()+")";
		}
	}
	
//	/**
//	 *  Get the copy info for method parameters.
//	 */
//	public static boolean[] getReferenceInfo(Method method, boolean refdef, boolean local)
//	{
//		boolean[] ret;
//		Object[] tmp = (Object[])methodreferences.get(method);
//		if(tmp!=null)
//		{
//			ret = (boolean[])tmp[local? 0: 1];
//		}
//		else
//		{
//			int params = method.getParameterTypes().length;
//			boolean[] localret = new boolean[params];
//			boolean[] remoteret = new boolean[params];
//			
//			for(int i=0; i<params; i++)
//			{
//				Annotation[][] ann = method.getParameterAnnotations();
//				localret[i] = refdef;
//				remoteret[i] = refdef;
//				for(int j=0; j<ann[i].length; j++)
//				{
//					if(ann[i][j] instanceof Reference)
//					{
//						Reference nc = (Reference)ann[i][j];
//						localret[i] = nc.local();
//						remoteret[i] = nc.remote();
//						break;
//					}
//				}
//			}
//			
//			methodreferences.put(method, new Object[]{localret, remoteret});
//			ret = local? localret: remoteret;
//		}
//		return ret;
//	}
}
