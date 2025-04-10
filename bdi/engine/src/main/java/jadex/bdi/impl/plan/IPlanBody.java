package jadex.bdi.impl.plan;

import jadex.future.IFuture;

/**
 *  Interface for plan body.
 */
public interface IPlanBody
{
//	/**
//	 *  Get the plan body.
//	 */
//	public Object getBody();
	
	/**
	 *  Execute the plan body.
	 */
	public IFuture<?> executePlan(RPlan rplan);
}
