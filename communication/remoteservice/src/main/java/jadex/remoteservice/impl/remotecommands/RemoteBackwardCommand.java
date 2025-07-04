package jadex.remoteservice.impl.remotecommands;

import jadex.core.IComponent;
import jadex.future.IBackwardCommandFuture;
import jadex.future.IFuture;
import jadex.messaging.ISecurityInfo;
import jadex.remoteservice.IRemoteConversationCommand;

/**
 *  Command for sending backward command data.
 */
public class RemoteBackwardCommand<T> extends AbstractIdSenderCommand implements IRemoteConversationCommand<T>
{
	/** The backward command info. */
	protected Object info;
	
	/**
	 *  Create the command.
	 */
	public RemoteBackwardCommand()
	{
		// Bean constructor
	}
	
	/**
	 *  Create the command.
	 */
	public RemoteBackwardCommand(Object info)
	{
		this.info = info;
	}
	
	/**
	 *  Execute a command.
	 *  @param component The component to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public void	execute(IComponent component, IFuture<T> future, ISecurityInfo secinf)
	{
		((IBackwardCommandFuture)future).sendBackwardCommand(info);
	}
	
	/**
	 *  Get the info.
	 *  @return The info.
	 */
	public Object getInfo()
	{
		return info;
	}

	/**
	 *  Set the info.
	 *  @param info The info to set.
	 */
	public void setInfo(Object info)
	{
		this.info = info;
	}
	
}
