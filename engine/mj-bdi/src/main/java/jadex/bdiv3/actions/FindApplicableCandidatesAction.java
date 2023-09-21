package jadex.bdiv3.actions;

import jadex.bdiv3.runtime.impl.APL;
import jadex.bdiv3.runtime.impl.RGoal;
import jadex.bdiv3.runtime.impl.RProcessableElement;
import jadex.mj.feature.execution.IMjExecutionFeature;

/**
 *  Find applicable candidates action - searches plans for a goal/event.
 */
public class FindApplicableCandidatesAction implements Runnable
{
	/** The processable element. */
	protected RProcessableElement element;
	
	/**
	 *  Create a new action.
	 */
	public FindApplicableCandidatesAction(RProcessableElement element)
	{
		this.element = element;
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
				&& RGoal.GoalProcessingState.INPROCESS.equals(rgoal.getProcessingState());
		}
			
//		if(!ret)
//			System.out.println("not valid: "+this+" "+element);
		
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
//		if(element.toString().indexOf("cnp_make_proposal")!=-1)
//			System.out.println("Select app cands for: "+element.getId());
		
//		if(element!=null && element.toString().indexOf("testgoal")!=-1)
//			System.out.println("find applicable candidates: "+element);
		
//		System.out.println("find applicable candidates 1: "+element);
		final APL apl = element.getApplicablePlanList();
		apl.build();
		if(apl.isEmpty())
		{
//					if(element.toString().indexOf("go_home")!=-1)
//						System.out.println("find applicable candidates 2a: "+element.getId()+" "+apl);
			element.setState(RProcessableElement.State.NOCANDIDATES);
			element.planFinished(null);
//					element.reason(ia);
		}
		else
		{
//					if(element.toString().indexOf("go_home")!=-1)
//						System.out.println("find applicable candidates 2b: "+element.getId()+" "+apl);
			element.setState(RProcessableElement.State.APLAVAILABLE);
			IMjExecutionFeature.get().scheduleStep(new SelectCandidatesAction(element));
		}
	}
}
