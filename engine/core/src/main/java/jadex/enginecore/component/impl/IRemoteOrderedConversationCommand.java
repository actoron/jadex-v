package jadex.enginecore.component.impl;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.service.types.security.ISecurityInfo;

/**
 *  Interface for intermediate (or final) commands in existing conversations. 
 */
public interface IRemoteOrderedConversationCommand
{
	/**
	 *  Execute a command.
	 *  @param access The agent to run the command on.
	 *  @param conv The active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public void	execute(IInternalAccess access, IOrderedConversation conv, ISecurityInfo secinf);
}
