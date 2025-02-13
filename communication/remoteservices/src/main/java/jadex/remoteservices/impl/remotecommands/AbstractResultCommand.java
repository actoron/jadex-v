package jadex.remoteservices.impl.remotecommands;

import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.messaging.ISecurityInfo;

import java.util.Map;
import java.util.PriorityQueue;

/**
 *  Base class for Jadex built-in remote commands.
 *  Handles result counter for intermediate results.
 */
public abstract class AbstractResultCommand extends AbstractInternalRemoteCommand implements IRemoteOrderedConversationCommand
{
	/** Count of the result for ordering. */
	protected Integer resultcount;
	
	/**
	 *  Create a remote command.
	 */
	public AbstractResultCommand()
	{
		// Bean constructor.
	}
	
	/**
	 *  Create a remote command.
	 */
	public AbstractResultCommand(Map<String, Object> nonfunc)
	{
		super(nonfunc);
	}
	
	/**
	 *  Gets the result count.
	 *   
	 *  @return The result count.
	 */
	public Integer getResultCount()
	{
		return resultcount;
	}
	
	/**
	 *  Sets the result count.
	 *  
	 *  @param resultcount The result count.
	 */
	public void setResultCount(Integer resultcount)
	{
		this.resultcount = resultcount;
	}
	
	/**
	 *  Execute a command.
	 *  @param component The agent to run the command on.
	 *  @param conv The active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public void	execute(IComponent component, IOrderedConversation conv, ISecurityInfo secinf)
	{
		PriorityQueue<AbstractResultCommand> dc = conv.getDeferredCommands();
		dc.offer(this);
		
		AbstractResultCommand next = dc.peek();
		while (next != null && (next.getResultCount() == null || next.getResultCount() == conv.getNextResultCount()))
		{
			dc.poll();
			next.doExecute(component, conv.getFuture(), secinf);
			if (next.getResultCount() != null)
				conv.incNextResultCount();
			next = dc.peek();
		}
	}
	
	/**
	 *  Execute a command.
	 *  @param component The agent to run the command on.
	 *  @param future Future of the active conversation.
	 *  @param secinf The established security level to decide if the command is allowed.
	 */
	public abstract void doExecute(IComponent component, IFuture<?> future, ISecurityInfo secinf);
}
