package jadex.enginecore.component.impl;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.service.types.security.ISecurityInfo;
import jadex.future.IFuture;

/**
 *  Interface for intermediate (or final) commands in existing conversations. 
 */
public interface IRemoteConversationCommand<T>
{
	/**
	 *  Execute a command.
	 *  @param access The agent to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public void	execute(IInternalAccess access, IFuture<T> future, ISecurityInfo secinf);
}
