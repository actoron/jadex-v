package jadex.communication.impl.security.handshake;

import jadex.core.ComponentIdentifier.GlobalProcessIdentifier;

/**
 *  Message signaling the rejection of the handshake.
 *
 */
public class HandshakeRejectionMessage extends BasicSecurityMessage
{
	/**
	 *  Creates the message.
	 */
	public HandshakeRejectionMessage()
	{
	}
	
	/**
	 *  Creates the message.
	 */
	public HandshakeRejectionMessage(GlobalProcessIdentifier sender, String conversationid)
	{
		super(sender, conversationid);
	}
}
