package jadex.enginecore.component;

import jadex.enginecore.IComponentIdentifier;
import jadex.future.IFuture;

/**
 *  Feature for securely sending and handling remote execution commands.
 */
public interface IRemoteExecutionFeature
{
	/**
	 *  Execute a command on a remote agent.
	 *  @param target	The component to send the command to.
	 *  @param command	The command to be executed.
	 *  @param clazz	The return type.
	 *  @param timeout	Custom timeout or null for default.
	 *  @return	The result(s) of the command, if any.
	 */
	public <T> IFuture<T>	execute(IComponentIdentifier target, IRemoteCommand<T> command, Class<? extends IFuture<T>> clazz, Long timeout);
}
