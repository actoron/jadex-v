package jadex.bridge.component.impl.remotecommands;

import java.util.Map;

import jadex.bridge.IInternalAccess;
import jadex.bridge.service.types.security.ISecurityInfo;
import jadex.commons.future.IFuture;
import jadex.commons.future.IntermediateFuture;

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
	public RemoteFinishedCommand(Map<String, Object> nonfunc)
	{
		super(nonfunc);
	}
	
	/**
	 *  Execute a command.
	 *  @param access The agent to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	@SuppressWarnings("unchecked")
	public void	doExecute(IInternalAccess access, IFuture<?> future, ISecurityInfo secinf)
	{
		((IntermediateFuture<T>)future).setFinished();
	}
}
