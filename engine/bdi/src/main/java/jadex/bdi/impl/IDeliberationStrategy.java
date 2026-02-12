package jadex.bdi.impl;

import jadex.bdi.impl.goal.RGoal;
import jadex.future.IFuture;

/**
 *  Interface for goal deliberation strategies.
 */
// TODO: should not use RGoal!?
public interface IDeliberationStrategy
{
	/**
	 *  Init the strategy.
	 *  @param agent The agent.
	 */
	public void init();
	
	/**
	 *  Called when a goal has been adopted.
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsAdopted(RGoal goal);
	
	/**
	 *  Called when a goal has been dropped.
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsDropped(RGoal goal);
	
	/**
	 *  Called when a goal becomes an option.
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsOption(RGoal goal);
	
	/**
	 *  Called when a goal becomes active.
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsActive(RGoal goal);
	
	/**
	 *  Called when a goal is not active any longer (suspended or option).
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsNotActive(RGoal goal);
}
