package jadex.remoteservices.impl.remotecommands;

import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.messaging.ISecurityInfo;
import jadex.remoteservices.IRemoteConversationCommand;

/**
 *  Command for future termmination.
 */
public class RemoteTerminationCommand<T> extends AbstractIdSenderCommand implements IRemoteConversationCommand<T>
{
	/** The termination reason (if any). */
	protected Exception reason;
	
	/**
	 *  Create the command.
	 */
	public RemoteTerminationCommand()
	{
	}
	
	/**
	 *  Create the command.
	 */
	public RemoteTerminationCommand(Exception reason)
	{
		this.reason = reason;
	}
	
	/**
	 *  Execute a command.
	 *  @param component The agent to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public void	execute(IComponent component, IFuture<T> future, ISecurityInfo secinf)
	{
		if (reason!=null)
		{
			((ITerminableFuture<T>)future).terminate(reason);
		}
		else
		{
			((ITerminableFuture<T>)future).terminate();
		}
	}
	
	/**
	 *  Get the reason.
	 *  @return The reason.
	 */
	public Exception getReason()
	{
		return reason;
	}

	/**
	 *  Set the reason.
	 *  @param reason The reason to set.
	 */
	public void setReason(Exception reason)
	{
		this.reason = reason;
	}
	
}
