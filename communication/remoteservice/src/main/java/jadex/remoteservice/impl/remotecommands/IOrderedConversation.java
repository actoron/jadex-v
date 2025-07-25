package jadex.remoteservice.impl.remotecommands;

import jadex.future.IFuture;

import java.util.PriorityQueue;

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
