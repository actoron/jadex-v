package jadex.bdi.blocksworld;

import jadex.bdi.IGoal;
import jadex.bdi.IPlan;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanPrecondition;
import jadex.bdi.blocksworld.BlocksworldAgent.ClearGoal;
import jadex.bdi.impl.goal.RGoal;

/**
 *  Clear a block.
 */
@Plan
public class ClearBlocksPlan extends StackBlocksPlan
{
    /**
     *
     */
    @PlanPrecondition
    public boolean checkExistsBlock(IPlan plan)
    {
    	return ((ClearGoal)((IGoal)plan.getReason()).getPojo()).getBlock().getUpper()!=null;
    	// TODO: doesn not work, because fields are not injected yet
    	// Do not want to add extra object just for precondition, because is executes onstart/end
//        boolean ret = getBlock()!=null;
//        return ret;
    }

	/**
	 * 
	 */
	public Block getBlock()
	{
		return ((ClearGoal)((RGoal)rplan.getReason()).getPojo()).getBlock().getUpper();
	}
	
	/**
	 * 
	 */
	public Block getTarget()
	{
		return ((ClearGoal)((RGoal)rplan.getReason()).getPojo()).getTarget();
	}
}

