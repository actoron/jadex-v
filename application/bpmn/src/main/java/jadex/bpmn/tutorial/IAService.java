package jadex.bpmn.tutorial;

import jadex.future.IFuture;

/**
 * 
 */
public interface IAService
{
	/**
	 * 
	 */
	public IFuture<String> appendHello(String text);
}
