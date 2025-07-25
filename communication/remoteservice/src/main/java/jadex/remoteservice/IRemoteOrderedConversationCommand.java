package jadex.remoteservice;

import jadex.core.IComponent;
import jadex.messaging.ISecurityInfo;
import jadex.remoteservice.impl.IIdSenderCommand;
import jadex.remoteservice.impl.remotecommands.IOrderedConversation;

/**
 *  Interface for intermediate (or final) commands in existing conversations. 
 */
public interface IRemoteOrderedConversationCommand extends IIdSenderCommand
{
	/**
	 *  Execute a command.
	 *  @param component The agent to run the command on.
	 *  @param conv The active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public void	execute(IComponent component, IOrderedConversation conv, ISecurityInfo secinf);
}
