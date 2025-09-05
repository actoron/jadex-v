package jadex.networking.impl.transport;

import jadex.core.ComponentIdentifier;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.future.IFuture;

/**
 *  Generic interface for transports.
 */
public interface ITransport
{
    /**
     *  Sends a message to a remote host.
     *
     *  @param receiver Message receiver.
     *  @param rawmessage The raw message.
     *  @return Null, when sent.
     */
    public IFuture<Void> sendMessage(ComponentIdentifier receiver, byte[] rawmessage);

    /**
     *  Gets the priority of the transport.
     *  @return Priority of the transport, higher means higher priority.
     */
    public int getPriority();

    /**
     *  Starts the transport server for incoming connections (if applicable).
     */
    public void startServer();
}
