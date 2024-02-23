package jadex.providedservice.impl.service.interceptors;

import jadex.providedservice.impl.service.IServiceInvocationInterceptor;
import jadex.providedservice.impl.service.ServiceInvocationContext;

/**
 *  Simple abstract base class that implements isApplicable with true.
 */
public abstract class AbstractApplicableInterceptor implements IServiceInvocationInterceptor
{
	/**
	 *  Test if the interceptor is applicable.
	 *  @return True, if applicable.
	 */
	public boolean isApplicable(ServiceInvocationContext context)
	{
		return true;
	}
	
}
