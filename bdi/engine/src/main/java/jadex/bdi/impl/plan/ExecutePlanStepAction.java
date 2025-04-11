package jadex.bdi.impl.plan;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.goal.RProcessableElement;
import jadex.future.IFuture;
import jadex.future.IResultListener;

// todo: use IPlan (and plan executor abstract to be able to execute plans as subcomponents)
// todo: allow using multi-step plans

/**
 *  Action for executing a plan step. 
 */
public class ExecutePlanStepAction implements Runnable
{
	/** The plan. */
	protected RPlan rplan;
	
	/**
	 *  Create a new action.
	 */
	public ExecutePlanStepAction(RPlan rplan)
	{
		this.rplan = rplan;
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		// todo: abort execution
		boolean ret = RPlan.PlanLifecycleState.NEW.equals(rplan.getLifecycleState())
			|| RPlan.PlanLifecycleState.BODY.equals(rplan.getLifecycleState());
		
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
		
//		final Object element = rplan.getReason();
//		if(element instanceof RGoal)
//		{
//			RGoal rgoal = (RGoal)element;
//			
////			System.out.println("executing candidate: "+rplan+" "+rgoal.getLifecycleState()+" "+rgoal.getProcessingState());
//			
//			if(!(RGoal.GoalLifecycleState.ACTIVE.equals(rgoal.getLifecycleState())
//				&& RGoal.GoalProcessingState.INPROCESS.equals(rgoal.getProcessingState())))
//			{
//				// todo: hack, how to avoid side effect
////				rplan.abort();
//				return;
//			}
//		}
		
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
//				// Set plan as child of goal
//				if(element instanceof RGoal)
//				{
//					RGoal rgoal = (RGoal)element;
//					rgoal.setChildPlan(rplan);
//				}
				
				// Force creation of pojo before adding plan to agent.
				rplan.getBody().createPojo(rplan);
				
				// TODO: capability?
				((BDIAgentFeature)rplan.getComponent().getFeature(IBDIAgentFeature.class)).addPlan(rplan);
				
				@SuppressWarnings("unchecked")
				IFuture<Object>	ret	= (IFuture<Object>) rplan.getBody().executePlan(rplan);
				if(ret!=null)
				{
					ret.addResultListener(new IResultListener<Object>()
					{
						public void resultAvailable(Object result)
						{
//							IInternalBDIAgentFeature.get().getCapability().removePlan(rplan);
							Object reason = rplan.getReason();
							if(reason instanceof RProcessableElement)
							{
								((RProcessableElement)reason).planFinished(rplan);
							}
						}
						
						public void exceptionOccurred(Exception exception)
						{
							resultAvailable(null);
						}
					});
				}
				else
				{
//					IInternalBDIAgentFeature.get().getCapability().removePlan(rplan);
					Object reason = rplan.getReason();
					if(reason instanceof RProcessableElement)
					{
						((RProcessableElement)reason).planFinished(rplan);
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
