package jadex.messaging;

import jadex.core.ComponentIdentifier;
import jadex.future.IFuture;


/**
 *  Feature for sending messages and handling incoming messages via handlers.
 */
public interface IMessageFeature
{
	/**
	 *  Send a message.
	 *  @param message	The message.
	 *  @param receiver	The message receiver(s). At least one required unless given in message object (e.g. FipaMessage).
	 *  
	 */
	public IFuture<Void> sendMessage(Object message, ComponentIdentifier... receiver);
	
	/**
	 *  Send a message and wait for a reply.
	 *  
	 *  @param receiver	The message receiver.
	 *  @param message	The message.
	 *  
	 *  @return The reply.
	 */
	public IFuture<SecureExchange> sendMessageAndWait(ComponentIdentifier receiver, Object message);
	
	/**
	 *  Send a message reply.
	 *  @param conversationid ID of the received message that is being replied to, see RequestMessage.
	 *  @param reply The reply message.
	 *  
	 */
	public IFuture<Void> sendReply(ComponentIdentifier receiver, String conversationid, Object reply);
	
	/**
	 *  Add a message handler.
	 *  @param  The handler.
	 */
	public void addMessageHandler(IMessageHandler handler);
	
	/**
	 *  Remove a message handler.
	 *  @param handler The handler.
	 */
	public void removeMessageHandler(IMessageHandler handler);
}
