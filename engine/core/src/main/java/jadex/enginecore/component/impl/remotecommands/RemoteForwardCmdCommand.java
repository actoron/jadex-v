package jadex.enginecore.component.impl.remotecommands;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.service.types.security.ISecurityInfo;
import jadex.future.IForwardCommandFuture;
import jadex.future.IFuture;

/**
 *  Remote command for sending future commands in ICommandFuture.
 */
public class RemoteForwardCmdCommand extends AbstractResultCommand //implements IRemoteConversationCommand<Object>
{
	/** The Command. */
	protected Object	command;
	
	/**
	 *  Create the command.
	 */
	public RemoteForwardCmdCommand()
	{
		// Bean constructor.
	}
	
	/**
	 *  Create the command.
	 */
	public RemoteForwardCmdCommand(Object command)
	{
		this.command = command;
	}
	
	/**
	 *  Execute a command.
	 *  @param access The agent to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public void	doExecute(IInternalAccess access, IFuture<?> future, ISecurityInfo secinf)
	{
		((IForwardCommandFuture)future).sendForwardCommand(command);
	}
	
	/**
	 *  Get the command.
	 *  @return the command.
	 */
	public Object	getCommand()
	{
		return command;
	}

	/**
	 *  Set the command.
	 *  @param command The result to set.
	 */
	public void setCommand(Object command)
	{
		this.command = command;
	}
}
