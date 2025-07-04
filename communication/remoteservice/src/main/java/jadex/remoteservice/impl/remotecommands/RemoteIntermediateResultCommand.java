package jadex.remoteservice.impl.remotecommands;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.future.IntermediateFuture;
import jadex.messaging.ISecurityInfo;

import java.util.Map;

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
	public RemoteIntermediateResultCommand(String id, ComponentIdentifier sender, T result, Map<String, Object> nonfunc)
	{
		super(id, sender, nonfunc);
		this.result = result;
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
