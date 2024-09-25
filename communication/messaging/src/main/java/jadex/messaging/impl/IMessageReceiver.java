package jadex.messaging.impl;

import jadex.messaging.ISecurityInfo;

public interface IMessageReceiver
{
	/**
	 *  Handle a message that arrived.
	 *  @param secinfos Security information.
	 *  @param msg The message.
	 */
	public void messageArrived(ISecurityInfo secinfos, Object msg);
}
