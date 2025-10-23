package jadex.bdi.impl.plan;

import jadex.bdi.impl.goal.RGoal;
import jadex.bdi.impl.goal.RProcessableElement;

// todo: use IPlan (and plan executor abstract to be able to execute plans as subcomponents)
// todo: allow using multi-step plans

/**
 *  Action for executing a plan step. 
 */
public class ExecutePlanStepAction implements Runnable
{
	/** The plan. */
	protected RPlan rplan;
	
	/** The MR cycle number of the goal, if any. */
	protected int	mrcycle;
	
	/**
	 *  Create a new action.
	 */
	public ExecutePlanStepAction(RPlan rplan)
	{
		this.rplan = rplan;
		Object element = rplan.getReason();
		if(element instanceof RGoal)
		{
			this.mrcycle	= ((RGoal)element).getMRCycle(); 
		}
//		System.out.println("schedule: "+this+", "+mrcycle);
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		boolean ret = RPlan.PlanLifecycleState.NEW.equals(rplan.getLifecycleState())
			|| RPlan.PlanLifecycleState.BODY.equals(rplan.getLifecycleState());
		
		if(ret)
		{
			ret = isReasonValid();
		}
//		System.out.println("isvalid: "+this+", "+ret+", "+mrcycle);
		
		return ret;
	}

	/**
	 *  Check if the reason for this plan is still in same means-end reasoning cycle.
	 */
	protected boolean isReasonValid()
	{
		boolean	ret	= true;
		Object element = rplan.getReason();
		if(element instanceof RGoal)
		{
			RGoal rgoal = (RGoal)element;
			ret = RGoal.GoalLifecycleState.ACTIVE.equals(rgoal.getLifecycleState())
				&& RGoal.GoalProcessingState.INPROCESS.equals(rgoal.getProcessingState())
				&& mrcycle==rgoal.getMRCycle();
		}
		return ret;
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
		
		// Initial context condition evaluation
		// Checks the context condition also directly before a plan is executed
		// Otherwise the rule might trigger only after the next state change (event)
		boolean context	= checkContextCondition();
		if(!context)
		{
			rplan.abort();
			if(rplan.getReason() instanceof RProcessableElement)
			{
				((RProcessableElement)rplan.getReason()).planFinished(null);
			}
		}
		else
		{
			// A new plan body must only be executed if it hasn't been aborted 
			if(!rplan.isFinishing() && RPlan.PlanLifecycleState.NEW.equals(rplan.getLifecycleState()))
			{
				// Set plan as child of goal
				Object element = rplan.getReason();
				if(element instanceof RGoal)
				{
					RGoal rgoal = (RGoal)element;
					rgoal.setChildPlan(rplan);
				}
				
//				System.out.println("execute: "+rplan.getComponent().getPojo()+"; "+this);
				
				rplan.getBody().executePlan(rplan);
				
				if(rplan.isTerminate())
				{
					rplan.getComponent().terminate();
				}
//				if(ret!=null)
//				{
//					ret.addResultListener(new IResultListener<Object>()
//					{
//						public void resultAvailable(Object result)
//						{
////							IInternalBDIAgentFeature.get().getCapability().removePlan(rplan);
//							Object reason = rplan.getReason();
//							if(reason instanceof RProcessableElement)
//							{
//								((RProcessableElement)reason).planFinished(rplan);
//							}
//						}
//						
//						public void exceptionOccurred(Exception exception)
//						{
//							resultAvailable(null);
//						}
//					});
//				}
//				else
				{
//					IInternalBDIAgentFeature.get().getCapability().removePlan(rplan);
					Object reason = rplan.getReason();
					if(reason instanceof RProcessableElement)
					{
						// Only inform about finished plan when still in same reasoning cycle (cf. GoalFlickerTest)
						if(isReasonValid())
						{
							((RProcessableElement)reason).planFinished(rplan);
						}
//						else
//						{
//							System.out.println("invalid: "+reason);
//						}
					}
				}
			}
			// Only needs to to something for waiting and new plans
			// Should processing state be set back to ready in case the plan is not within a step?
	//		else
	//		{
	//			System.out.println("Plan proc state invalid: "+rplan.getProcessingState()+" "+rplan);
	//		}
		}
	}
	
	/**
	 *  Check the context condition.
	 *  @return True if context is ok.
	 */
	protected boolean	checkContextCondition()
	{
		return rplan.getBody().checkContextCondition(rplan);
	}
	
	/**
	 *  Return a string.
	 */
	public String	toString()
	{
		return "ExecutePlanStepAction("+rplan+")";
	}
}
