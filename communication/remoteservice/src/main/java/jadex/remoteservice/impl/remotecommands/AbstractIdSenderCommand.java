package jadex.remoteservice.impl.remotecommands;

import jadex.core.ComponentIdentifier;
import jadex.remoteservice.impl.IIdSenderCommand;

/**
 *  Command that includes a conversation ID.
 */
public abstract class AbstractIdSenderCommand implements IIdSenderCommand
{
    /** The remote invocation ID. */
    private String id;

    /** Sender of the command. */
    private ComponentIdentifier sender;

    public AbstractIdSenderCommand(String rxid, ComponentIdentifier sender)
    {
        this.id = rxid;
        this.sender = sender;
    }

    /**
     *  Get the conversation ID of the command.
     *  @return The ID.
     */
    public String getId()
    {
        return id;
    }

    /**
     *  Sets the conversation ID of the command.
     * @param id The id.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     *  Gets the component sending the message.
     *  @return The component sending the message.
     */
    public ComponentIdentifier getSender()
    {
        return sender;
    }

    /**
     *  Sets the component sending the message.
     *  @param sender The component sending the message.
     */
    public void setSender(ComponentIdentifier sender)
    {
        this.sender = sender;
    }
}
