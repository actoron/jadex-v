package jadex.provided2.impl;

import jadex.future.IFuture;

/**
 *  Service invocation interceptor interface.
 */
public interface IServiceInvocationInterceptor
{
	/**
	 *  Execute the interceptor.
	 *  @param context The invocation context.
	 */
	public IFuture<Void> execute(ServiceInvocationContext context); 
	
	/**
	 *  Test if the interceptor is applicable.
	 *  @return True, if applicable.
	 */
	public boolean isApplicable(ServiceInvocationContext context);
}
