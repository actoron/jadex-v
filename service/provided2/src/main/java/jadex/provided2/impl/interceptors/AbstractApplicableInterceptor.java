package jadex.provided2.impl.interceptors;

import jadex.provided2.impl.IServiceInvocationInterceptor;
import jadex.provided2.impl.ServiceInvocationContext;

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
