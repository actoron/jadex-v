package jadex.bdi.runtime.impl;

import java.util.Collections;

import jadex.bdi.model.MPlan;
import jadex.common.MethodInfo;
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
	
//	/** The resume command. */
//	protected ICommand<Boolean> rescom;
	
	/**
	 *  Create a new action.
	 */
	public ExecutePlanStepAction(RPlan rplan)
	{
//		this(rplan, null);
		this.rplan = rplan;
	}
	
//	/**
//	 *  Create a new action.
//	 */
//	public ExecutePlanStepAction(RPlan rplan, ICommand<Boolean> rescom)
//	{
////		System.err.println("epsa: "+rplan);
////		Thread.dumpStack();
////		this.element = element;
//		this.rplan = rplan;
//		this.rescom = rescom;
//	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		// todo: abort execution
		boolean ret = RPlan.PlanLifecycleState.NEW.equals(rplan.getLifecycleState())
			|| RPlan.PlanLifecycleState.BODY.equals(rplan.getLifecycleState());
		
//		if(ret)
//		{
//			Object element = rplan.getReason();
//			if(element instanceof RGoal)
//			{
//				RGoal rgoal = (RGoal)element;
//				ret = RGoal.GOALLIFECYCLESTATE_ACTIVE.equals(rgoal.getLifecycleState())
//					&& RGoal.GOALPROCESSINGSTATE_INPROCESS.equals(rgoal.getProcessingState());
//				// todo: hack, how to avoid side effect
//				if(!ret)
//					rplan.abort();
//			}
//		}
			
//		if(!ret)
//			System.out.println("not valid: "+rplan);
		
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
		
		//if(rplan.toString().indexOf("cnp_make_proposal")!=-1)
		//	System.out.println("plan exe: "+rplan);
		
		// problem plan context for steps needed that allows to know
		// when a plan has completed 
		
		final Object element = rplan.getReason();
		if(element instanceof RGoal)
		{
			RGoal rgoal = (RGoal)element;
			
//			System.out.println("executing candidate: "+rplan+" "+rgoal.getLifecycleState()+" "+rgoal.getProcessingState());
			
			if(!(RGoal.GoalLifecycleState.ACTIVE.equals(rgoal.getLifecycleState())
				&& RGoal.GoalProcessingState.INPROCESS.equals(rgoal.getProcessingState())))
			{
				// todo: hack, how to avoid side effect
//				rplan.abort();
				return;
			}
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
			// Rescom now directly executed 
	//		if(RPlan.PlanProcessingState.WAITING.equals(rplan.getProcessingState()))
	//		{
	//			rescom.execute(null);
	////			rplan.continueAfterWait(rescom);
	//		}else if
			// A new plan body must only be executed if it hasn't been aborted 
			if(!rplan.isFinishing() && RPlan.PlanLifecycleState.NEW.equals(rplan.getLifecycleState()))
			{
				// Set plan as child of goal
				if(element instanceof RGoal)
				{
					RGoal rgoal = (RGoal)element;
					rgoal.setChildPlan(rplan);
				}
				
//					System.out.println("execute plan: "+rplan);
				
	//			final BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
				IInternalBDIAgentFeature.get().getCapability().addPlan(rplan);
				
				IPlanBody body = rplan.getBody();
				body.executePlan().addResultListener(new IResultListener<Void>()
				{
					public void resultAvailable(Void result)
					{
						IInternalBDIAgentFeature.get().getCapability().removePlan(rplan);
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
			// Only needs to to something for waiting and new plans
			// Should processing state be set back to ready in case the plan is not within a step?
	//		else
	//		{
	//			System.out.println("Plan proc state invalid: "+rplan.getProcessingState()+" "+rplan);
	//		}
		}
	}
	
	/**
	 *  Get the rplan.
	 */
	public RPlan	getRPlan()
	{
		return rplan;
	}
	
	/**
	 *  Check the context condition.
	 *  @return True if context is ok.
	 */
	protected boolean	checkContextCondition()
	{
		// Check context condition initially, allows for fast abort before first step
		MPlan mplan = (MPlan)rplan.getModelElement();
		final MethodInfo mi = mplan.getBody().getContextConditionMethod(rplan.getBody().getClass().getClassLoader());
		boolean context = true;
		if(mi!=null)
		{
			context	= BDILifecycleAgentFeature.invokeBooleanMethod(rplan.getBody().getBody(),
				mi.getMethod(rplan.getBody().getClass().getClassLoader()), mplan, null, rplan);
		}
		else if(mplan.getContextCondition()!=null)
		{
			context = BDILifecycleAgentFeature.evaluateCondition(mplan.getContextCondition(), rplan.getModelElement(), 
				Collections.singletonMap(rplan.getFetcherName(), (Object)rplan));
		}
//		System.out.println("context cond: "+context+" "+mplan.getName());
		return context;
	}
	
//	/**
//	 * 
//	 */
//	protected boolean isReasonValid()
//	{
//		boolean ret = true;
//		Object element = rplan.getReason();
//		if(element instanceof RGoal)
//		{
//			RGoal rgoal = (RGoal)element;
//			ret = IGoal.GoalLifecycleState.ACTIVE.equals(rgoal.getLifecycleState())
//				&& IGoal.GoalProcessingState.INPROCESS.equals(rgoal.getProcessingState());
//		}
//		return ret;
//	}
	
	/**
	 *  Return a string.
	 */
	public String	toString()
	{
		return "ExecutePlanStepAction("+rplan+")";
	}
}
