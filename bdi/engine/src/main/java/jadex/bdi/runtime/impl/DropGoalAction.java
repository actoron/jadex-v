package jadex.bdi.runtime.impl;

import jadex.future.IResultListener;

/**
 * 
 */
public class DropGoalAction implements Runnable
{
	/** The goal. */
	protected RGoal goal;
	
	/**
	 *  Create a new action.
	 */
	public DropGoalAction(RGoal goal)
	{
		this.goal = goal;
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		return RGoal.GoalLifecycleState.DROPPING.equals(goal.getLifecycleState());
	}
	
	/**
	 *  Execute the command.
	 */
	public void	run()
	{
		if(!isValid())
		{
			return;
		}
//		BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
//		goal.unobserveGoal(ia);
		
		goal.callFinishedMethod().addResultListener(new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
				cont();
			}
			
			public void exceptionOccurred(Exception exception)
			{
				cont();
			}
			
			protected void cont()
			{
				IInternalBDIAgentFeature.get().getCapability().removeGoal(goal);
				goal.setLifecycleState(RGoal.GoalLifecycleState.DROPPED);
			}
		});
	}
}
