package jadex.bdi;

import jadex.rules.eca.ChangeInfo;


/**
 *  Listener for observing beliefs.
 */
public interface IBeliefListener<T>
{	
//	/**
//	 *  Invoked when a belief has been changed.
//	 *  @param event The change event.
//	 */ 
//	public void beliefChanged(ChangeInfo<T> info);
//	
	/**
	 *  Invoked when a fact has been added.
	 *  The new fact is contained in the agent event.
	 *  @param event The change event.
	 */
	public default void factAdded(ChangeInfo<T> info) {}

	/**
	 *  Invoked when a fact has been removed.
	 *  The removed fact is contained in the agent event.
	 *  @param event The change event.
	 */
	public default void factRemoved(ChangeInfo<T> info) {}

	/**
	 *  Invoked when a fact in a belief set has changed (i.e. bean event).
	 *  @param value The new value.
	 *  @param oldvalue The old value.
	 *  @param info Extra info (such as the index of the element if applicable).
	 */ 
	public default void factChanged(ChangeInfo<T> info) {}

}
