package jadex.mj.feature.providedservice.impl.service.impl.interceptors;

import jadex.mj.core.impl.MjComponent;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.providedservice.impl.service.impl.ServiceInvocationContext;

/**
 *  Ensures that interceptor is only called when component thread is in the chain.
 */
public abstract class ComponentThreadInterceptor extends AbstractApplicableInterceptor
{
	/** The internal access. */
	protected MjComponent ia;	
	
	/**
	 *  Create a new ComponentThreadInterceptor. 
	 */
	public ComponentThreadInterceptor(MjComponent ia)
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

		return getComponent().getFeature(IMjExecutionFeature.class).isComponentThread();
	}
	
	/**
	 *  Get the component.
	 */
	public MjComponent getComponent()
	{
		return ia;
	}
}
