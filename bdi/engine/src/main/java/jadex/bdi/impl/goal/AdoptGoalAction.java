package jadex.bdi.impl.goal;

import jadex.bdi.impl.plan.RPlan.PlanLifecycleState;

/**
 *  Action for adopting a goal.
 */
public class AdoptGoalAction implements Runnable
{
	/** The goal. */
	protected RGoal goal;
	
	/** The state of the plan (if any), when the goal was dispatched.
	 *  Action becomes invalid, when plan state changes (e.g. from body to passed) */
	protected PlanLifecycleState planstate;
	
	/**
	 *  Create a new action.
	 */
	public AdoptGoalAction(RGoal goal)
	{
		this.goal = goal;
		
		// todo: support this also for a parent goal?!
		if(goal.getParentPlan()!=null)
		{
			this.planstate = goal.getParentPlan().getLifecycleState();
		}
//		System.out.println("adopting: "+goal+", "+planstate);
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		return (planstate==null || planstate.equals(goal.getParentPlan().getLifecycleState())) 
			&& RGoal.GoalLifecycleState.NEW.equals(goal.getLifecycleState());
	}
	
	/**
	 *  Execute the action.
	 */
	public void	run()
	{
		if(isValid())
		{
//			System.out.println("adopted: "+goal+", "+planstate);
			goal.adopt();
		}
		// else action no longer required
//		else
//		{
//			System.out.println("not adopted: "+goal+", "+planstate);
//		}
	}
}
