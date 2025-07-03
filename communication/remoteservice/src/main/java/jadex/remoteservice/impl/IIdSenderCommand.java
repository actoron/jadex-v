package jadex.remoteservice.impl;

import jadex.core.ComponentIdentifier;

/**
 *  Interface for remote conversations carrying an ID and a sender.
 */
public interface IIdSenderCommand
{
    /**
     *  Get the conversation ID of the command.
     *  @return The ID.
     */
    public String getId();

    /**
     *  Gets the component sending the message.
     *  @return The component sending the message.
     */
    public ComponentIdentifier getSender();
}
