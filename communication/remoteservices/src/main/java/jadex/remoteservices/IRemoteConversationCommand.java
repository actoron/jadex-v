package jadex.remoteservices;

import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.messaging.ISecurityInfo;
import jadex.remoteservices.impl.IIdSenderCommand;

/**
 *  Interface for intermediate (or final) commands in existing conversations. 
 */
public interface IRemoteConversationCommand<T> extends IIdSenderCommand
{
	/**
	 *  Execute a command.
	 *  @param access The agent to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public void	execute(IComponent access, IFuture<T> future, ISecurityInfo secinf);
}
