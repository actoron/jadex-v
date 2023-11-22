package jadex.bpmn.runtime.handler;

import jadex.future.IFuture;

/**
 * 
 */
public interface ICancelable
{
	/**
	 * 
	 */
	public IFuture<Void> cancel();
}
