package jadex.remoteservices.impl.remotecommands;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.future.IntermediateFuture;
import jadex.messaging.ISecurityInfo;

import java.util.Map;


/**
 *  Command for finished intermediate futures.
 */
public class RemoteFinishedCommand<T>	extends AbstractResultCommand
{
	/**
	 *  Create the command.
	 */
	public RemoteFinishedCommand()
	{
	}
	
	/**
	 *  Create the command.
	 */
	public RemoteFinishedCommand(String id, ComponentIdentifier sender, Map<String, Object> nonfunc)
	{
		super(id, sender, nonfunc);
	}
	
	/**
	 *  Execute a command.
	 *  @param component The component to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	@SuppressWarnings("unchecked")
	public void	doExecute(IComponent component, IFuture<?> future, ISecurityInfo secinf)
	{
		((IntermediateFuture<T>)future).setFinished();
	}
}
