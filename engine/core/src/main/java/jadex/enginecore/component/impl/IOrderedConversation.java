package jadex.enginecore.component.impl;

import java.util.PriorityQueue;

import jadex.enginecore.component.impl.remotecommands.AbstractResultCommand;
import jadex.future.IFuture;

/**
 *  Interface for a remote conversation in progress that processes
 *  ordered commands.
 *
 */
public interface IOrderedConversation
{
	/**
	 *  Gets the conversation result future.
	 *  
	 *  @return The future.
	 */
	public IFuture<?> getFuture();
	
	/**
	 *  Gets the count of the next result.
	 *  
	 *  @return The count of the next result.
	 */
	public int getNextResultCount();
	
	/**
	 *  Increases the next result count. 
	 */
	public void incNextResultCount();
	
	/**
	 *  Returns queue of commands that have been deferred due to
	 *  out-of-order arrival.
	 *  
	 *  @return Queue of commands.
	 */
	public PriorityQueue<AbstractResultCommand> getDeferredCommands();
}
