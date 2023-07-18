package jadex.enginecore.service.component.interceptors;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.ProxyFactory;
import jadex.enginecore.ServiceCall;
import jadex.enginecore.nonfunctional.INFPropertyProvider;
import jadex.enginecore.service.IInternalService;
import jadex.enginecore.service.IService;
import jadex.enginecore.service.ServiceInvalidException;
import jadex.enginecore.service.component.BasicServiceInvocationHandler;
import jadex.enginecore.service.component.ServiceInfo;
import jadex.enginecore.service.component.ServiceInvocationContext;
import jadex.future.DelegationResultListener;
import jadex.future.ExceptionDelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  The validation interceptor tests whether a service call is valid.
 *  
 *  Calls isValid() on IService to check if service call is allowed.
 */
public class ValidationInterceptor extends ComponentThreadInterceptor
{
	//-------- constants --------
	
	/** The static map of subinterceptors (method -> interceptor). */
	protected static final Set ALWAYSOK;
	
	static
	{
		try
		{
			ALWAYSOK = new HashSet();
			
			ALWAYSOK.add(Object.class.getMethod("toString", new Class[0]));
			ALWAYSOK.add(Object.class.getMethod("equals", new Class[]{Object.class}));
			ALWAYSOK.add(Object.class.getMethod("hashCode", new Class[0]));
			
//			ALWAYSOK.add(IService.class.getMethod("getId", new Class[0]));
//			ALWAYSOK.add(IService.class.getMethod("isValid", new Class[0]));
			
			Method[] ms = IService.class.getDeclaredMethods();
			for(Method m: ms)
			{
				ALWAYSOK.add(m);
			}
			
			ms = IInternalService.class.getDeclaredMethods();
			for(Method m: ms)
			{
				ALWAYSOK.add(m);
			}
			
			ALWAYSOK.add(INFPropertyProvider.class.getMethod("shutdownNFPropertyProvider", new Class[0]));
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 *  Create a new interceptor.
	 */
	public ValidationInterceptor(IInternalAccess component)
	{
		super(component);
	}
	
//	/**
//	 *  Test if the interceptor is applicable.
//	 *  @return True, if applicable.
//	 */
//	public boolean isApplicable(ServiceInvocationContext context)
//	{
//		return true;
//	}
	
	/**
	 *  Execute the interceptor.
	 *  @param context The invocation context.
	 */
	public IFuture<Void> execute(final ServiceInvocationContext sic)
	{
		final Future<Void> ret = new Future<Void>();
		
		boolean scheduleable = sic.getMethod().getReturnType().equals(IFuture.class) 
		|| sic.getMethod().getReturnType().equals(void.class);

		if(!scheduleable || ALWAYSOK.contains(sic.getMethod()))
		{
			sic.invoke().addResultListener(new DelegationResultListener<Void>(ret));
		}
		else
		{
			// Call isValid() on proxy to execute full interceptor chain.
//			IService ser = (IService)sic.getProxy();
//			IFuture<Boolean>	valid	= ser.isValid();
			
			// Call isValid() directly for speed.
			BasicServiceInvocationHandler	handler	= (BasicServiceInvocationHandler)ProxyFactory.getInvocationHandler(sic.getProxy());
			Object	service	= handler.getService();
			IFuture<Boolean>	valid;
			
//			if(IComponentIdentifier.LOCAL.get()==null && sic.getMethod().getName().equals("status"))
//			{
//				System.out.println("null invocation: "+sic.getMethod());
//			}
//			Map<String, Object>	props	= new HashMap<String, Object>();
//			props.put("method1", sic.getMethod());
			ServiceCall sc = CallAccess.getOrCreateNextInvocation(null);//props);
			sc.setProperty(ServiceCall.MONITORING, Boolean.FALSE);
			sc.setProperty(ServiceCall.INHERIT, true);
//			CallAccess.setServiceCall(sc);
			if(service instanceof IService)
			{
				valid	= ((IService)service).isValid();
			}
			else
			{
				valid	= ((ServiceInfo)service).getManagementService().isValid();
			}
			
			valid.addResultListener(new ExceptionDelegationResultListener<Boolean, Void>(ret)
			{
				public void customResultAvailable(Boolean result)
				{
					if(result.booleanValue())
					{
						sic.invoke().addResultListener(new DelegationResultListener<Void>(ret));
					}
					else
					{
						ret.setException(new ServiceInvalidException(sic.getMethod().getName()));
					}
				}
			});
		}
		
		return ret;
	}
}
