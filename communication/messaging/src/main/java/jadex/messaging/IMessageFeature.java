package jadex.messaging;

import java.util.Map;

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
	 *  Send a message.
	 *  @param message	The message.
	 *  @param addheaderfields Additional header fields.
	 *  @param receiver	The message receiver(s). At least one required unless given in message object (e.g. FipaMessage).
	 *  
	 */
	public IFuture<Void> sendMessage(Object message, Map<String, Object> addheaderfields, ComponentIdentifier... receiver);
	
	/**
	 *  Send a message and wait for a reply.
	 *  
	 *  @param receiver	The message receiver.
	 *  @param message	The message.
	 *  
	 *  @return The reply.
	 */
	// Todo: intermediate future with multiple receivers?
	public IFuture<Object> sendMessageAndWait(ComponentIdentifier receiver, Object message);
	
	/**
	 *  Send a message and wait for a reply.
	 *  
	 *  @param receiver	The message receiver.
	 *  @param message	The message.
	 *  @param timeout	The reply timeout.
	 *  
	 *  @return The reply.
	 */
	// Todo: intermediate future with multiple receivers?
	public IFuture<Object> sendMessageAndWait(ComponentIdentifier receiver, Object message, Long timeout);
	
	/**
	 *  Send a message reply.
	 *  @param receivedmessageid	ID of the received message that is being replied to.
	 *  @param message	The reply message.
	 *  
	 */
	public IFuture<Void> sendReply(Object message);
	
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
	
	/**
	 *  Create a virtual output connection.
	 *  @param sender The sender.
	 *  @param receiver The receiver.
	 *  @param nonfunc The nonfunc props.
	 */
	//public IFuture<IOutputConnection> createOutputConnection(ComponentIdentifier sender, ComponentIdentifier receiver, Map<String, Object> nonfunc);

	/**
	 *  Create a virtual input connection.
	 *  @param sender The sender.
	 *  @param receiver The receiver.
	 *  @param nonfunc The nonfunc props.
	 */
	//public IFuture<IInputConnection> createInputConnection(ComponentIdentifier sender, ComponentIdentifier receiver, Map<String, Object> nonfunc);
}
