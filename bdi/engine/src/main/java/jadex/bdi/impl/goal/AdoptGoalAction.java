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
//		System.out.println("adopting: "+goal.getId()+" "+goal.getPojoElement().getClass().getName());
		this.goal = goal;
		
		// todo: support this also for a parent goal?!
		if(goal.getParentPlan()!=null)
		{
			this.planstate = goal.getParentPlan().getLifecycleState();
		}
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
			goal.adopt(null);
		}
		// else action no longer required
	}
}
