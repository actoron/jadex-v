package jadex.mj.feature.providedservice.impl.service.impl.interceptors;

import jadex.mj.feature.providedservice.impl.service.impl.IServiceInvocationInterceptor;
import jadex.mj.feature.providedservice.impl.service.impl.ServiceInvocationContext;

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
