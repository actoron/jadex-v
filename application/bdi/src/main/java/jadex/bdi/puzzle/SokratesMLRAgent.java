package jadex.bdi.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalAPLBuild;
import jadex.bdi.annotation.GoalSelectCandidate;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.impl.goal.ICandidateInfo;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
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
@BDIAgent
@Plan(trigger=@Trigger(goals=SokratesMLRAgent.ChooseMoveGoal.class), impl= ChooseMovePlan.class)
// TODO: binding options for pojo plans?
// TODO: trigger is only required for adding correct context fetcher
@Plan(trigger=@Trigger(goals=SokratesMLRAgent.MakeMoveGoal.class), impl=MovePlan.class)
public class SokratesMLRAgent
{
	@Belief
	IBoard	board	= new Board(5);
	int triescnt;
	long	delay	= 500;
	Strategy	ml	= Strategy.SAME_LONG;
	
	@Inject
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
	class ChooseMoveGoal	implements Supplier<ICandidateInfo>
	{
		List<ICandidateInfo>	cands;
		
//		@GoalResult
		ICandidateInfo	cand;
		
		public ChooseMoveGoal(List<ICandidateInfo> cands)
		{
			this.cands	= cands;
		}
		
		@Override
		public ICandidateInfo get()
		{
			return cand;
		}
	}
	
	/**
	 *  The agent body.
	 */
	@OnStart
	public void body(IExecutionFeature exe, IBDIAgentFeature plan)
	{
		SwingUtilities.invokeLater(() -> new BoardGui(exe.getComponent().getComponentHandle(), board));
		
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
		IComponentManager.get().create(new SokratesMLRAgent()).get();
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
