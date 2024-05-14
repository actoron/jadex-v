package jadex.bdi.puzzle;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Body;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalAPLBuild;
import jadex.bdi.annotation.GoalResult;
import jadex.bdi.annotation.GoalSelectCandidate;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Plans;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.IBDIAgent;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.impl.ICandidateInfo;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
/**
  <H3>Puzzling agent.</H3>
  
  This agent that plays a board game for one player.
  This example is a Jadex adaption of the original JACK(TM)
  puzzle example and was used for performance comparisons
  between both platforms (cf. readme.txt).
  This version shows how the puzzle works
  with a graphical board and uses a delay
  between the moves. Measurements were done
  with the Benchmark.agent in this package.
 */
@Agent(type="bdip")
@Plans({
	@Plan(trigger=@Trigger(goals = SokratesMLRAgent.ChooseMoveGoal.class), body = @Body(ChooseMovePlan.class)),
	// TODO: binding options for pojo plans?
	@Plan(body = @Body(MovePlan.class))})
public class SokratesMLRAgent
{
	@Belief
	IBoard	board	= new Board(5);
	int triescnt;
	long	delay	= 500;
	Strategy	ml	= Strategy.SAME_LONG;
	
	@Agent
	IComponent agent;
	
	enum Strategy
	{
		NONE, SHORT, LONG, SAME_LONG, ALTER_LONG;
	}
	
	@Goal
	class MakeMoveGoal
	{
		int	depth;
		
		MakeMoveGoal(int depth)
		{
			this.depth	= depth;
		}
		
		@GoalTargetCondition
		boolean	isSolution()
		{
			return board.isSolution();
		}
		
		@GoalAPLBuild
		List<Object>	buildAPL()
		{
			List<Object>	ret	= new ArrayList<>();
			for(Move move: board.getPossibleMoves())
			{
				ret.add(new MovePlan(move));
			}
			return ret;
		}
		
		@GoalSelectCandidate
		ICandidateInfo	chooseMove(IBDIAgentFeature bdi, List<ICandidateInfo> cands)
		{
			return (ICandidateInfo)bdi.dispatchTopLevelGoal(new ChooseMoveGoal(cands)).get();
		}
	}
	
	@Goal
	class ChooseMoveGoal
	{
		List<ICandidateInfo>	cands;
		
		@GoalResult
		ICandidateInfo	cand;
		
		public ChooseMoveGoal(List<ICandidateInfo> cands)
		{
			this.cands	= cands;
		}
	}
	
	/**
	 *  The agent body.
	 */
	@OnStart
	public void body(IExecutionFeature exe, IBDIAgentFeature plan)
	{
		SwingUtilities.invokeLater(() -> new BoardGui(exe.getComponent().getExternalAccess(), board));
		
		System.out.println("Now puzzling:");
		long	start	= exe.getTime();
		try
		{
			plan.dispatchTopLevelGoal(new MakeMoveGoal(0)).get();
		}
		catch(Exception gfe)
		{
			System.out.println("No solution found :-( "+gfe);
		}
		
		long end = exe.getTime();
		System.out.println("Needed: "+(end-start)+" millis.");

		agent.terminate();
	}


	public static void main(String[] args)
	{
		IBDIAgent.create(new SokratesMLRAgent());
	}
}
