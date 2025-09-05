package jadex.networking.impl;

import jadex.core.ComponentIdentifier;
import jadex.core.impl.GlobalProcessIdentifier;

/**
 *  Handler interface used by transport to process incoming messages.
 */
public interface IMessageHandler
{
    /**
     *  Handle incoming message.
     *
     *  @param senderhost Sender of the message from the transport perspective.
     *  @param receiver Receiver of the message from the transport perspective.
     *  @param message The raw message.
     */
    public void handleMessage(String senderhost, ComponentIdentifier receiver, byte[] message);
}
