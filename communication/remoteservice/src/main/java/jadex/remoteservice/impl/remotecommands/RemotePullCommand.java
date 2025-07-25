package jadex.remoteservice.impl.remotecommands;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.future.IPullIntermediateFuture;
import jadex.messaging.ISecurityInfo;
import jadex.remoteservice.IRemoteConversationCommand;

import java.util.Collection;


/**
 *  Command for pulling from pull intermediate futures.
 */
public class RemotePullCommand<T> extends AbstractIdSenderCommand implements IRemoteConversationCommand<Collection<T>>
{
	/**
	 *  Create the command.
	 */
	public RemotePullCommand()
	{
		super(null, null);
	}

	/**
	 *  Create the command.
	 */
	public RemotePullCommand(String rxid, ComponentIdentifier sender)
	{
		super(rxid, sender);
	}
	
	/**
	 *  Execute a command.
	 *  @param component The agent to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public void	execute(IComponent component, IFuture<Collection<T>> future, ISecurityInfo secinf)
	{
		((IPullIntermediateFuture<T>)future).pullIntermediateResult();
	}
}
