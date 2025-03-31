package jadex.providedservice.impl.service.interceptors;

import java.util.Collection;

import jadex.common.MethodInfo;
import jadex.core.IComponent;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IIntermediateResultListener;
import jadex.future.IResultListener;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.IntermediateEmptyResultListener;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.impl.service.ProvidedServiceFeature;
import jadex.providedservice.impl.service.ServiceInvocationContext;

/**
 *  Interceptor for observing method calls start and end e.g. for timing.
 */
public class MethodCallListenerInterceptor extends AbstractApplicableInterceptor
{
	//-------- methods --------

	/** The component. */
	protected IComponent comp;
	
	/** The service. */
	protected IService service;
	
	/**
	 *  Create a new interceptor.
	 */
	public MethodCallListenerInterceptor(IComponent comp, IService service)
	{
		this.comp	= comp;
		this.service = service;
	}
	
	/**
	 *  Test if the interceptor is applicable.
	 *  @return True, if applicable.
	 */
	public boolean isApplicable(ServiceInvocationContext context)
	{
		// Interceptor is used in both chains, provided and required
//		if(context.getMethod().getName().indexOf("methodA")!=-1)
//			System.out.println("interceptor: "+component.getComponentIdentifier());
//		boolean ret = component.getServiceContainer().hasMethodListeners(sid, new MethodInfo(context.getMethod()));
		boolean ret = super.isApplicable(context) && ((ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class)).hasMethodListeners(service.getServiceId(), new MethodInfo(context.getMethod()));
//		System.out.println("app: "+context.getMethod().getName()+" "+ret);
		return ret;
	}
	
	/**
	 *  Execute the interceptor.
	 *  @param context The invocation context.
	 */
	public IFuture<Void> execute(final ServiceInvocationContext sic)
	{
//		System.out.println("method call lis start: "+sic.hashCode());
		Future<Void> ret = new Future<Void>();
		((ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class)).notifyMethodListeners(service, true, sic.getMethod(), sic.getArgumentArray(), sic.hashCode());
		sic.invoke().addResultListener(new DelegationResultListener<Void>(ret)
		{
			public void customResultAvailable(Void result)
			{
//				if(sic.getMethod().getName().indexOf("destroy")!=-1)
//					System.out.println("hhhhuuuuu");
//				System.out.println("method call lis end: "+sic.hashCode());
				Object	res	= sic.getResult();
				if(res instanceof IIntermediateFuture)
				{
					IIntermediateResultListener<Object> lis = new IntermediateEmptyResultListener<Object>()
					{
						public void finished()
						{
							((ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class)).notifyMethodListeners(service, false, sic.getMethod(), sic.getArgumentArray(), sic.hashCode());
						}
						
						public void resultAvailable(Collection<Object> result)
						{
							((ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class)).notifyMethodListeners(service, false, sic.getMethod(), sic.getArgumentArray(), sic.hashCode());
						}
						
						public void exceptionOccurred(Exception exception)
						{
							((ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class)).notifyMethodListeners(service, false, sic.getMethod(), sic.getArgumentArray(), sic.hashCode());
						}
					};
					
					if(res instanceof ISubscriptionIntermediateFuture)
					{
						((ISubscriptionIntermediateFuture)res).addQuietListener(lis);
					}
					else
					{
						((IIntermediateFuture)res).addResultListener(lis);
					}
				}
				else if(res instanceof IFuture)
				{
					((IFuture<Object>)res).addResultListener(new IResultListener<Object>()
					{
						public void resultAvailable(Object result)
						{
							((ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class)).notifyMethodListeners(service, false, sic.getMethod(), sic.getArgumentArray(), sic.hashCode());
						}
						
						public void exceptionOccurred(Exception exception)
						{
							((ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class)).notifyMethodListeners(service, false, sic.getMethod(), sic.getArgumentArray(), sic.hashCode());
						}
					});
				}
				else
				{
					((ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class)).notifyMethodListeners(service, false, sic.getMethod(), sic.getArgumentArray(), sic.hashCode());
				}
				super.customResultAvailable(result);
			}
		});
		return ret;
	}
}