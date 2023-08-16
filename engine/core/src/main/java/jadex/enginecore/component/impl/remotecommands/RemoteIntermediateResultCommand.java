package jadex.enginecore.component.impl.remotecommands;

import java.util.Map;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.service.types.security.ISecurityInfo;
import jadex.future.IFuture;
import jadex.future.IntermediateFuture;

/**
 * Command for intermediate results.
 */
public class RemoteIntermediateResultCommand<T>	extends AbstractResultCommand
{
	/** The result. */
	protected T result;
	
	/**
	 *  Create the command.
	 */
	public RemoteIntermediateResultCommand()
	{
	}
	
	/**
	 *  Create the command.
	 */
	public RemoteIntermediateResultCommand(T result, Map<String, Object> nonfunc)
	{
		super(nonfunc);
		this.result = result;
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
		((IntermediateFuture<T>)future).addIntermediateResult(result);
	}
	
	/**
	 *  Get the result.
	 *  @return the result.
	 */
	public T getIntermediateResult()
	{
		return result;
	}

	/**
	 *  Set the result.
	 *  @param result The result to set.
	 */
	public void setIntermediateResult(T result)
	{
		this.result = result;
	}
}
