package jadex.bdi.puzzle;

import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanFailed;
import jadex.bdi.annotation.PlanPassed;
import jadex.bdi.puzzle.SokratesMLRAgent.MakeMoveGoal;
import jadex.bdi.runtime.IPlan;


/**
 *  Make a move and dispatch a subgoal for the next.
 */
@Plan
public class MovePlan
{
	/** The move to perform. */
	Move	move;
	
	/**
	 *  Create a plan to perform a certain move.
	 */
	MovePlan(Move move)
	{
		this.move	= move;
	}
	
	/**
	 *  The plan body.
	 */
	@PlanBody
	void body(SokratesMLRAgent agent, MakeMoveGoal goal, IPlan plan)
	{
		agent.triescnt++;
		print("Trying "+move+" ("+agent.triescnt+") ", goal.depth);
		agent.board.move(move);
		
		plan.waitFor(agent.delay).get();
		
		plan.dispatchSubgoal(agent.new MakeMoveGoal(goal.depth+1)).get();
	}

	/**
	 *  The plan failure code.
	 */
	@PlanFailed
	void failed(SokratesMLRAgent agent, MakeMoveGoal goal, IPlan plan)
	{
		print("Failed "+move, goal.depth);
		agent.board.takeback();
		plan.waitFor(agent.delay).get();
	}

	/**
	 *  The plan passed code.
	 */
	@PlanPassed
	void passed(IPlan plan, MakeMoveGoal goal)
	{
		print("Succeeded "+move, goal.depth);
	}

	/**
	 *  The plan aborted code.
	 */
	@PlanAborted
	void aborted(IPlan plan, MakeMoveGoal goal)
	{
		print("Aborted "+move, goal.depth);
	}

	/**
	 *  Print out an indented string.
	 *  @param text The text.
	 *  @param indent The number of cols to indent.
	 */
	void print(String text, int indent)
    {
        for(int x=0; x<indent; x++)
            System.out.print(" ");
        System.out.println(text);
    }
	
	@Override
	public String toString()
	{
		return "MovePlan("+move+")";
	}
}
