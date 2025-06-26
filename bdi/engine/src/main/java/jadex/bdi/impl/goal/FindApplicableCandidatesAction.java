package jadex.bdi.impl.goal;

import jadex.execution.IExecutionFeature;

/**
 *  Find applicable candidates action - searches plans for a goal/event.
 */
public class FindApplicableCandidatesAction implements Runnable
{
	/** The processable element. */
	protected RProcessableElement element;
	
	/** The MR cycle number of the goal, if any. */
	protected int	mrcycle;
	
	/**
	 *  Create a new action.
	 */
	public FindApplicableCandidatesAction(RProcessableElement element)
	{
		this.element = element;
//		System.out.println("schedule: "+this);
		
		if(element instanceof RGoal)
		{
			this.mrcycle	= ((RGoal)element).getMRCycle(); 
		}
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		boolean ret = true;
		
		if(element instanceof RGoal)
		{
			RGoal rgoal = (RGoal)element;
			ret = RGoal.GoalLifecycleState.ACTIVE.equals(rgoal.getLifecycleState())
					&& RGoal.GoalProcessingState.INPROCESS.equals(rgoal.getProcessingState())
					&& mrcycle==rgoal.getMRCycle();
		}
		
//		System.out.println("isvalid: "+this+", "+ret);
		
		return ret;
	}
	
	/**
	 *  Execute the command.
	 *  @param args The argument(s) for the call.
	 *  @return The result of the command.
	 */
	public void run()
	{
		if(!isValid())
		{
			return;
		}
		
		final APL apl = element.getApplicablePlanList();
		apl.build();
		if(apl.isEmpty())
		{
			element.planFinished(null);
		}
		else
		{
			element.getComponent().getFeature(IExecutionFeature.class).scheduleStep(new SelectCandidatesAction(element));
		}
	}
}
