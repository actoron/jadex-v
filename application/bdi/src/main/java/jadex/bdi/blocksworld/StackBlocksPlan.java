package jadex.bdi.blocksworld;

import jadex.bdi.IPlan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.blocksworld.BlocksworldAgent.ClearGoal;
import jadex.bdi.blocksworld.BlocksworldAgent.StackGoal;
import jadex.bdi.impl.goal.RGoal;
import jadex.injection.annotation.Inject;


/**
 *  Stack a block on top of another.
 */
//@Plan
public class StackBlocksPlan	
{
	//-------- attributes --------

	@Inject
	protected BlocksworldAgent capa;
	
	@Inject
	protected IPlan rplan;
	
	//-------- methods --------

	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{
		// Clear blocks.
		
		try
		{
//		System.out.println(getClass().getName()+" "+getBlock()+" "+getTarget());
		
		ClearGoal clear = capa.new ClearGoal(getBlock());
		rplan.dispatchSubgoal(clear).get();
		
		clear = capa.new ClearGoal(getTarget());
		rplan.dispatchSubgoal(clear).get();

		
		// Maybe wait before moving block.
		if(capa.getMode().equals(BlocksworldAgent.Mode.SLOW))
		{
			rplan.waitFor(1000).get();
//			waitFor(1000);
		}
		else if(capa.getMode().equals(BlocksworldAgent.Mode.STEP))
		{
			capa.steps.getNextIntermediateResult();
//			waitForInternalEvent("step");
		}

		// Now move block.
		if(!capa.isQuiet())
			System.out.println("Moving '"+getBlock()+"' to '"+getTarget()+"'");

		// This operation has to be performed atomic,
		// because it fires bean changes on several affected blocks. 
		rplan.startAtomic();
		getBlock().stackOn(getTarget());
		rplan.endAtomic();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public Block getBlock()
	{
		return ((StackGoal)((RGoal)rplan.getReason()).getPojo()).getBlock();
	}
	
	/**
	 * 
	 */
	public Block getTarget()
	{
		return ((StackGoal)((RGoal)rplan.getReason()).getPojo()).getTarget();
	}
}
