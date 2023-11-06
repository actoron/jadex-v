package jadex.bdi.runtime.impl;

import jadex.future.IFuture;

/**
 *  Interface for plan body.
 */
public interface IPlanBody
{
	/**
	 *  Get the plan body.
	 */
	public Object getBody();
	
	/**
	 *  Execute the plan body.
	 */
	public IFuture<Void> executePlan();
	
	/**
	 *  Issue abortion of the plan body, if currently running.
	 */
	public void abort();
}
