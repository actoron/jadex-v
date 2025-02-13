package jadex.remoteservices;


import jadex.core.IComponent;
import jadex.messaging.ISecurityInfo;
import jadex.future.IFuture;

/**
 *  Interface for remotely executable commands.
 */
public interface IRemoteCommand<T>
{
	/**
	 *  Execute a command.
	 *  @param component The agent that is running the command.
	 *  @param secinf The established security level to e.g. decide if the command is allowed.
	 *  @return A future for return value(s). May also be intermediate, subscription, etc.
	 */
	public IFuture<T> execute(IComponent component, ISecurityInfo secinf);
	
	/**
	 *  Checks if the remote command is internally valid.
	 * 
	 *  @param component The component access.
	 *  @return Exception describing the error if invalid.
	 */
	public Exception isValid(IComponent component);

	public String getId();
}
