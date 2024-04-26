package jadex.nfproperty.sensor.service;

import java.lang.reflect.Method;

import jadex.providedservice.IMethodInvocationListener;
import jadex.providedservice.impl.service.ServiceInvocationContext;
import jadex.providedservice.impl.service.interceptors.ResolveInterceptor;

/**
 *  Listener that only observes user methods. It ignores calls to
 *  IService, INFPropertyProvider and INMethodPropertyProvider interfaces.
 */
public class UserMethodInvocationListener implements IMethodInvocationListener
{
	/** The protected listener. */
	protected IMethodInvocationListener listener;
	
	/**
	 *  Create a new UserMethodInvocationListener. 
	 */
	public UserMethodInvocationListener(IMethodInvocationListener listener)
	{
		this.listener = listener;
	}

	/**
	 *  Called when a method call started.
	 */
	public void methodCallStarted(Object proxy, Method method, final Object[] args, Object callid, ServiceInvocationContext context)
	{
		if(ResolveInterceptor.SERVICEMETHODS.contains(method))
		{
			return;
		}
		else
		{
			listener.methodCallStarted(proxy, method, args, callid, context);
		}
	}
	
	/**
	 *  Called when the method call is finished.
	 */
	public void methodCallFinished(Object proxy, Method method, final Object[] args, Object callid, ServiceInvocationContext context)
	{
		if(ResolveInterceptor.SERVICEMETHODS.contains(method))
		{
			return;
		}
		else
		{
			listener.methodCallFinished(proxy, method, args, callid, context);
		}
	}
}
