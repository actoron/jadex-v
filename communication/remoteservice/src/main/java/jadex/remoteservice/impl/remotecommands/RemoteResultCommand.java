package jadex.remoteservice.impl.remotecommands;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.messaging.ISecurityInfo;

import java.util.Map;


/**
 * Command for results.
 */
public class RemoteResultCommand<T>	extends AbstractResultCommand
{
	/** The result. */
	protected T result;

	/** The exception. */
	protected Exception exception;
	
	/**
	 *  Create the command.
	 */
	public RemoteResultCommand()
	{
	}
	
	/**
	 *  Create the command.
	 */
	public RemoteResultCommand(String id, ComponentIdentifier sender, T result, Map<String, Object> nonfunc)
	{
		super(id, sender, nonfunc);
		setResult(result);
	}
	
	/**
	 *  Create the command.
	 */
	public RemoteResultCommand(String id, ComponentIdentifier sender, Exception exception, Map<String, Object> nonfunc)
	{
		super(id, sender, nonfunc);
		this.exception = exception;
	}
	
	/**
	 *  Execute a command.
	 *  @param component The agent to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	@SuppressWarnings("unchecked")
	public void	doExecute(IComponent component, IFuture<?> future, ISecurityInfo secinf)
	{
//		if(result!=null)// && result.toString().indexOf("rt")!=-1)
//			System.out.println("exe result: "+result.getClass()+" "+result+" "+access.getId());
		
		if(exception!=null)
			((Future<T>)future).setException(exception);
		else
			((Future<T>)future).setResult(result);
	}
	
	/**
	 *  Get the result.
	 *  @return the result.
	 */
	public T getResult()
	{
		return result;
	}

	/**
	 *  Set the result.
	 *  @param result The result to set.
	 */
	public void setResult(T result)
	{
//		if(result!=null && result.getClass().getName().indexOf("Connection")!=-1)
//			System.out.println("rescom with: "+result);
		
//		if(result!=null)
//			System.out.println("created rrc: "+result.getClass()+" "+result);
//		Thread.dumpStack();
		this.result = result;
	}
	
	/**
	 *  Get the exception.
	 *  @return The exception.
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 *  Set the exception.
	 *  @param exception The exception to set.
	 */
	public void setException(Exception exception)
	{
		this.exception = exception;
	}
	
	/**
	 *  Get a string representation.
	 */
	public String	toString()
	{
		return "RemoteResultCommand("+(exception!=null?"ex: "+exception:result)+")";
	}
}
