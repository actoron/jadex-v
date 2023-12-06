package jadex.communication.impl.security.handshake;

import jadex.core.ComponentIdentifier.GlobalProcessIdentifier;

/**
 *  Base class for security messages.
 *
 */
public class BasicSecurityMessage
{
	/** The message sender. */
	protected GlobalProcessIdentifier sender;
	
	/** The conversation ID. */
	protected String conversationid;
	
	/** The unique message ID to filter duplicates. */
	protected String messageid;
	
	/**
	 *  Create message.
	 */
	public BasicSecurityMessage()
	{
	}
	
	/**
	 *  Create message.
	 */
	public BasicSecurityMessage(GlobalProcessIdentifier sender, String conversationid)
	{
		this.sender = sender;
		this.conversationid = conversationid;
	}

	/**
	 *  Gets the sender.
	 * 
	 *  @return The sender
	 */
	public GlobalProcessIdentifier getSender()
	{
		return sender;
	}

	/**
	 *  Sets the sender.
	 * 
	 *  @param sender The sender to set.
	 */
	public void setSender(GlobalProcessIdentifier sender)
	{
		this.sender = sender;
	}
	
	/**
	 *  Gets the conversation ID.
	 *  
	 *  @return The conversation ID.
	 */
	public String getConversationId()
	{
		return conversationid;
	}
	
	/**
	 *  Sets the conversation ID.
	 *  
	 *  @param conversationid The conversation ID.
	 */
	public void setConversationId(String conversationid)
	{
		this.conversationid = conversationid;
	}
	
	/**
	 *  Get the message ID.
	 *  @return Message ID.
	 */
	public String getMessageId()
	{
		return messageid;
	}
	
	/**
	 *  Sets the message ID.
	 *  @param messageid The message ID.
	 */
	public void setMessageId(String messageid)
	{
		this.messageid = messageid;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName()+"(sender="+getSender()+", convid="+getConversationId()+")";
	}
}
