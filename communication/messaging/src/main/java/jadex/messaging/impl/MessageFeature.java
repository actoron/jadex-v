package jadex.messaging.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentManager;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.messaging.IIpcFeature;
import jadex.messaging.IMessageFeature;
import jadex.messaging.IMessageHandler;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.ISecurityFeature.DecodedMessage;
import jadex.messaging.ISecurityInfo;
import jadex.messaging.SecureExchange;
import jadex.messaging.ipc.IpcStreamHandler;
import jadex.messaging.security.SecurityFeature;
import jadex.serialization.SerializationServices;

public class MessageFeature implements IMessageFeature
{
	/** Messages awaiting reply. */
	protected Map<String, Future<SecureExchange>> awaitingmessages;
	
	/** The list of message handlers. */
	protected Set<IMessageHandler> messagehandlers;
	
	/** The component. */
	protected Component self;
	
	/**
	 *  Creates the feature.
	 *  
	 *  @param self The component offering the feature.
	 */
	public MessageFeature(Component self)
	{
		this.awaitingmessages = new HashMap<>();
		this.messagehandlers = new HashSet<>();
		this.self = self;
	}
	
	/**
	 *  Send a message.
	 *  @param message	The message.
	 *  @param receivers	The message receiver(s). At least one required unless given in message object (e.g. FipaMessage).
	 *  
	 */
	public IFuture<Void> sendMessage(Object message, ComponentIdentifier... receivers)
	{
		for (ComponentIdentifier receiver : receivers)
		{
			if (GlobalProcessIdentifier.getSelf().equals(receiver.getGlobalProcessIdentifier()))
			{
				// Local message
				IComponentHandle exta = ComponentManager.get().getComponent(receiver).getComponentHandle();
				exta.scheduleStep((comp) ->
				{
					((MessageFeature) comp.getFeature(IMessageFeature.class)).messageArrived(null, message);
				});
			}
			else
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				SerializationServices.get().encode(baos, IComponentManager.get().getClassLoader(), message);
				SecurityFeature sec = (SecurityFeature) IComponentManager.get().getFeature(ISecurityFeature.class);
				byte[] emsg = sec.encryptAndSign(receiver, baos.toByteArray());
				baos = null;
				IpcStreamHandler ipc = (IpcStreamHandler) IComponentManager.get().getFeature(IIpcFeature.class);
				ipc.sendMessage(receiver, emsg);
			}
		}
		return IFuture.DONE;
	}
	
	/**
	 *  Send a message and wait for a reply.
	 *  
	 *  @param receiver	The message receiver.
	 *  @param message	The message.
	 *  
	 *  @return The reply.
	 */
	public IFuture<SecureExchange> sendMessageAndWait(ComponentIdentifier receiver, Object message)
	{
		Future<SecureExchange> ret = new Future<>();
		
		String convid = sendExchangeMessage(receiver, null, message);
		awaitingmessages.put(convid, ret);
		
		return ret;
	}
	
	/**
	 *  Send a message reply.
	 *  @param receivedmessageid ID of the received message that is being replied to.
	 *  @param message The reply message.
	 *  
	 */
	public IFuture<Void> sendReply(ComponentIdentifier receiver, String conversationid, Object reply)
	{
		sendExchangeMessage(receiver, conversationid, reply);
		return IFuture.DONE;
	}
	
	/**
	 *  Add a message handler.
	 *  @param  The handler.
	 */
	public void addMessageHandler(IMessageHandler handler)
	{
		messagehandlers.add(handler);
	}
	
	/**
	 *  Remove a message handler.
	 *  @param handler The handler.
	 */
	public void removeMessageHandler(IMessageHandler handler)
	{
		messagehandlers.remove(handler);
	}
	
	/**
	 *  Inform the component that a message has arrived.
	 *  
	 *  @param secinfos The security meta infos.
	 *  @param msg The message that arrived.
	 */
	public void messageArrived(ISecurityInfo secinfos, Object msg)
	{
		if (msg instanceof TransmittedExchange)
		{
			TransmittedExchange tex = (TransmittedExchange) msg;
			msg = new SecureExchange(tex.sender(), secinfos, tex.conversationid(), tex.message());
			
			Future<SecureExchange> awaitingmessage = awaitingmessages.remove(tex.conversationid());
			if (awaitingmessage != null)
			{
				awaitingmessage.setResultIfUndone((SecureExchange) msg);
				return;
			}
		}
		
		for (IMessageHandler handler : messagehandlers)
		{
			if (handler.isHandling(secinfos, msg))
			{
				handler.handleMessage(secinfos, msg);
				break;
			}
		}
	}
	
	/**
	 *  Inform the component that an encoded (serialized) message has arrived.
	 *  Called directly for intra-platform message delivery (i.e. local messages)
	 *  and indirectly for remote messages.
	 *  
	 *  @param origin Message origin.
	 *  @param encmsg The encoded message that arrived.
	 */
	public void externalMessageArrived(GlobalProcessIdentifier origin, byte[] encmsg)
	{
		SecurityFeature sec = (SecurityFeature) IComponentManager.get().getFeature(ISecurityFeature.class);
		DecodedMessage decmsg = sec.decryptAndAuth(origin, encmsg);
		encmsg = null;
		Object msg = SerializationServices.get().decode(new ByteArrayInputStream(decmsg.message(), 0, decmsg.message().length), IComponentManager.get().getClassLoader());
		messageArrived(decmsg.secinfo(), msg);
	}
	
	/**
	 *  Sends a request/response exchange message.
	 *  
	 *  @param receiver Message receiver.
	 *  @param conversationid The conversation ID or null to generate one.
	 *  @param message The message.
	 *  @return The conversation ID.
	 */
	private String sendExchangeMessage(ComponentIdentifier receiver, String conversationid, Object message)
	{
		conversationid = conversationid != null ? conversationid :SUtil.createUniqueId(self.toString());
		TransmittedExchange tex = new TransmittedExchange(self.getId(), conversationid, message);
		
		if (GlobalProcessIdentifier.getSelf().equals(receiver.getGlobalProcessIdentifier()))
		{
			// Local message
			IComponentHandle exta = ComponentManager.get().getComponent(receiver).getComponentHandle();
			exta.scheduleStep((comp) ->
			{
				((MessageFeature) comp.getFeature(IMessageFeature.class)).messageArrived(null, tex);
			});
		}
		else
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			SerializationServices.get().encode(baos, IComponentManager.get().getClassLoader(), tex);
			SecurityFeature sec = (SecurityFeature) IComponentManager.get().getFeature(ISecurityFeature.class);
			byte[] emsg = sec.encryptAndSign(receiver, baos.toByteArray());
			baos = null;
			IpcStreamHandler ipc = (IpcStreamHandler) IComponentManager.get().getFeature(IIpcFeature.class);
			ipc.sendMessage(receiver, emsg);
		}
		
		return conversationid;
	}
}
