package jadex.providedservice.impl.service.interceptors;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.ComponentManager;
import jadex.execution.future.ComponentFutureFunctionality;
import jadex.execution.future.FutureFunctionality;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.impl.service.ServiceInvocationContext;

/**
 *  The decoupling return interceptor ensures that the result
 *  notifications of a future a delivered on the calling 
 *  component thread.
 */
public class DecouplingReturnInterceptor extends AbstractApplicableInterceptor
{
	//-------- methods --------

	/**
	 *  Execute the interceptor.
	 */
	public IFuture<Void> execute(final ServiceInvocationContext sic)
	{
		Future<Void> fut	= new Future<Void>();
		
		// Schedule back to global runner, when not called from any component.
		final IComponent caller = IComponentManager.get().getCurrentComponent()!=null
			? IComponentManager.get().getCurrentComponent()
			: ComponentManager.get().getGlobalRunner();
				
		sic.invoke().addResultListener(new DelegationResultListener<Void>(fut)
		{
			public void customResultAvailable(Void result)
			{
//				if(sic.getMethod().getName().indexOf("getAllKnownNetworks")!=-1)
//					System.out.println("decouplingret: "+sic.getArguments());
				
				final Object res = sic.getResult();
				
				if(res instanceof IFuture)
				{
//					FutureFunctionality func = new FutureFunctionality()
//					{
//						@Override
//						public <T> void scheduleForward(final ICommand<T> com, final T args)
//						{
//							// Don't reschedule if already on correct thread.
//							if(caller==null || caller.getFeature(IExecutionFeature.class).isComponentThread())
//							{
//								com.execute(args);
//							}
//							else
//							{
//								//System.out.println("todo: scheduleDecoupledStep");
//								caller.getFeature(IExecutionFeature.class).scheduleStep(agent ->
//								{
//									com.execute(args);
//								});
//							}
//							/*else if (caller.getDescription().getState().equals(IComponentDescription.STATE_TERMINATED)
//									&& sic.getMethod().getName().equals("destroyComponent")
//									&& sic.getArguments().size()==1 && caller!=null && caller.getId().equals(sic.getArguments().get(0))) 
//							{
//								// do not try to reschedule if component killed itself and is already terminated to allow passing results to the original caller.
//								com.execute(args);
//							}*/
//							/*
//							else
//							{
//								final Exception ex	= Future.DEBUG ? new DebugException() : null;									
//								caller.getFeature(IMjExecutionFeature.class).scheduleDecoupledStep(new IComponentStep<Void>()
////								caller.getFeature(IExecutionFeature.class).scheduleStep(new ImmediateComponentStep<Void>()	// immediate was required for return of monitoring event component disposed. disabled waiting for last monitoring event instead. 
//								{
//									public IFuture<Void> execute(IInternalAccess ia)
//									{
//										if(ex!=null)
//										{
//											try
//											{
//												DebugException.ADDITIONAL.set(ex);
//												com.execute(args);
//												return IFuture.DONE;
//											}
//											finally
//											{
//												DebugException.ADDITIONAL.set(null);									
//											}
//										}
//										else
//										{
//											com.execute(args);
//											return IFuture.DONE;
//										}
//									}
//								}).addResultListener(new IResultListener<Void>()
//								{
//									public void resultAvailable(Void result) {}
//									
//									public void exceptionOccurred(Exception exception)
//									{
//										if(exception instanceof ComponentTerminatedException)
//										{
//											// pass exception back to future as receiver is already dead.
//											if(res instanceof ITerminableFuture<?>)
//											{
//												((ITerminableFuture<?>)res).terminate(exception);
//											}
//											else
//											{
//												getLogger().warning("Future receiver already dead: "+exception+", "+com+", "+res);
//											}
//										}
//										else
//										{
//											// shouldn't happen?
//											System.err.println("Unexpected Exception"+", "+com);
//											exception.printStackTrace();
//										}
//									}
//								});
//							}*/
//						}
//					};
					
//					String resstring	= sic.getMethod().getName().equals("getRegisteredClients") ? res.toString() : null;	// string before connect to see storeforfirst results
					
					@SuppressWarnings({"unchecked"})
					Future<Object> fut = (Future<Object>)FutureFunctionality.getDelegationFuture((IFuture<?>)res, new ComponentFutureFunctionality(caller));
					sic.setResult(fut);
					
//					if(sic.getMethod().getName().equals("getRegisteredClients"))
//					{
//						System.err.println("DecouplingReturnInterceptor getDelegationFuture: "+resstring+", "+fut+", "+IComponentIdentifier.LOCAL.get());
//						Thread.dumpStack();
//					}
					
					// Monitoring below.
					/*if(feat instanceof IInternalServiceMonitoringFeature && ((IInternalServiceMonitoringFeature)feat).isMonitoring())
					{
						if(!ServiceIdentifier.isSystemService(sic.getServiceIdentifier().getServiceType().getType(caller.getClassLoader())))
						{
							@SuppressWarnings({"rawtypes", "unchecked"})
							IResultListener<Object>	lis = new IIntermediateResultListener()
							{
	
								@Override
								public void exceptionOccurred(Exception exception)
								{
									((IInternalServiceMonitoringFeature)feat).postServiceEvent(
										new ServiceCallEvent(ServiceCallEvent.Type.EXCEPTION, sic.getServiceIdentifier(), new MethodInfo(sic.getMethod()), sic.getCaller(), exception));
								}
	
								@Override
								public void resultAvailable(Object result)
								{
									((IInternalServiceMonitoringFeature)feat).postServiceEvent(
										new ServiceCallEvent(ServiceCallEvent.Type.RESULT, sic.getServiceIdentifier(), new MethodInfo(sic.getMethod()), sic.getCaller(), result));
								}
	
								@Override
								public void intermediateResultAvailable(Object result)
								{
									((IInternalServiceMonitoringFeature)feat).postServiceEvent(
										new ServiceCallEvent(ServiceCallEvent.Type.INTERMEDIATE_RESULT, sic.getServiceIdentifier(), new MethodInfo(sic.getMethod()), sic.getCaller(), result));
								}
	
								@Override
								public void finished()
								{
									((IInternalServiceMonitoringFeature)feat).postServiceEvent(
										new ServiceCallEvent(ServiceCallEvent.Type.FINISHED, sic.getServiceIdentifier(), new MethodInfo(sic.getMethod()), sic.getCaller(), null));
								}
								
								@Override
								public void maxResultCountAvailable(int max) 
								{
									((IInternalServiceMonitoringFeature)feat).postServiceEvent(
										new ServiceCallEvent(ServiceCallEvent.Type.MAX, sic.getServiceIdentifier(), new MethodInfo(sic.getMethod()), sic.getCaller(), max));
								}
	
//								Not necessary?
//								@Override
//								public void resultAvailable(Collection<Object> result)
//								{
//									((IInternalServiceMonitoringFeature)feat).postServiceEvent(
//										new ServiceCallEvent(ServiceCallEvent.Type.RESULT, sic.getServiceIdentifier(), new MethodInfo(sic.getMethod()), sic.getCaller(), result));
//								}
								
							};
							fut.addResultListener(lis);
						}
					}*/
				}
				
				super.customResultAvailable(null);
			}
		});
		return fut; 
	}
}
