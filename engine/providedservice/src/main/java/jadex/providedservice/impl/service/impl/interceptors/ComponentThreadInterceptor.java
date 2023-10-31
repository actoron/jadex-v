package jadex.providedservice.impl.service.impl.interceptors;

import jadex.core.impl.Component;
import jadex.feature.execution.IExecutionFeature;
import jadex.providedservice.impl.service.impl.ServiceInvocationContext;

/**
 *  Ensures that interceptor is only called when component thread is in the chain.
 */
public abstract class ComponentThreadInterceptor extends AbstractApplicableInterceptor
{
	/** The internal access. */
	protected Component ia;	
	
	/**
	 *  Create a new ComponentThreadInterceptor. 
	 */
	public ComponentThreadInterceptor(Component ia)
	{
		this.ia = ia;
	}

	/**
	 *  Test if the interceptor is applicable.
	 *  @return True, if applicable.
	 */
	public boolean isApplicable(ServiceInvocationContext context)
	{
//		if(!getComponent().isComponentThread())
//			System.out.println("not on comp: "+context.getMethod().toString());
//			throw new RuntimeException("Must be called on component thread: "+Thread.currentThread());

		return getComponent().getFeature(IExecutionFeature.class).isComponentThread();
	}
	
	/**
	 *  Get the component.
	 */
	public Component getComponent()
	{
		return ia;
	}
}
