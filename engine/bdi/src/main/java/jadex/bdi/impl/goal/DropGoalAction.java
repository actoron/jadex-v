package jadex.bdi.impl.goal;

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
		
//		goal.callFinishedMethod().addResultListener(new IResultListener<Void>()
//		{
//			public void resultAvailable(Void result)
//			{
//				cont();
//			}
//			
//			public void exceptionOccurred(Exception exception)
//			{
//				cont();
//			}
//			
//			protected void cont()
//			{
				goal.setLifecycleState(RGoal.GoalLifecycleState.DROPPED);
//			}
//		});
	}
}
