package jadex.bdi.puzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalAPLBuild;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanFailed;
import jadex.bdi.annotation.PlanPassed;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.IBDIAgent;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IPlan;
import jadex.core.IComponent;
import jadex.future.DelegationResultListener;
import jadex.future.ExceptionDelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

/**
 *  Puzzle agent tries to solve a solitair board game
 *  by recursiveky applying means-end-reasoning.
 */
@Agent(type="bdip")
public class SokratesV3Agent
{
	//-------- attributes --------
	
	/** The puzzle board. */
	@Belief
	protected IBoard	board	= new JackBoard();
	
	/** The number of tried moves. */
	@Belief // needs not to be belief, just used here to test the BDIDebugger
	protected int	triescnt;
	
	/** The depth of the current move. */
	protected int	depth;
	
	/** The delay between two moves (in milliseconds). */
	protected long	delay	= 500;
	
	/** Print each step? */
	protected boolean	printsteps	= true;
	
	/** Print results? */
	protected boolean	printresults	= true;
	
	/** The strategy (none=choose the first applicable, long=prefer jump moves,
	 * same_long=prefer long moves of same color, alter_long=prefer long move of alternate color). */
	protected String strategy	= MoveComparator.STRATEGY_SAME_LONG;
	
	//-------- methods --------
	
	/**
	 *  Setup the gui and start playing.
	 */
	@OnStart
	public void	body(IComponent agent)
	{
//		strategy = agent.getConfiguration();
		createGui(agent);
		
		if(printresults)
		{
			System.out.println("strategy is: "+strategy);
			System.out.println("Now puzzling:");
		}
		
		final long	start	= System.currentTimeMillis();
		IFuture<MoveGoal> fut = agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new MoveGoal());
		fut.addResultListener(new IResultListener<MoveGoal>()
		{
			public void resultAvailable(MoveGoal movegoal)
			{
				long end = System.currentTimeMillis();
				if(printresults)
				{
					System.out.println("Needed: "+(end-start)+" millis.");
				}
				agent.terminate();
			}
			
			public void exceptionOccurred(Exception exception)
			{
				if(printresults)
				{
					System.out.println("No solution found :-(");
				}
				agent.terminate();
			}
		});
	}
	
	/**
	 *  Create the GUI (if any).
	 */
	protected void	createGui(final IComponent agent)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new BoardGui(agent.getComponentHandle(), board);
			}
		});
	}
	
	//-------- goals --------
	
	/**
	 *  The goal to make moves until the board reaches a solution.
	 */
	@Goal
	public class MoveGoal
	{
		/**
		 *  Move goal is successful when resulting board represents a solution.
		 */
		@GoalTargetCondition//(beliefs="board")
		public boolean	isAchieved()
		{
			return board.isSolution();
		}
		
		/**
		 *  Build plan candidates for all possible moves.
		 *  Sorts moves according to strategy.
		 */
		@GoalAPLBuild
		public List<MovePlan> buildAPL()
		{
			List<MovePlan>	ret	= new ArrayList<MovePlan>();
			List<Move>	moves	= board.getPossibleMoves();
//			System.out.println("cands0: "+moves);
			Collections.sort(moves, new MoveComparator(board, strategy));
			
			for(Move move: moves)
			{
				ret.add(new MovePlan(move));
			}
			
//			System.out.println("cands1: "+ret);
		
			return ret;
		}
	}
	
	//-------- plans --------
	
	/**
	 *  Plan to make a move.
	 */
	@Plan(trigger=@Trigger(goals=MoveGoal.class))
	public class MovePlan
	{
		//-------- attributes --------
		
		/** The move. */
		protected Move move;
		
		//-------- constructors --------
		
		/**
		 *  Create a move plan-
		 */
		public MovePlan(Move move)
		{
			this.move = move;
		}
		
		//-------- methods --------
		
		/**
		 *  The plan body.
		 */
		@PlanBody
		public IFuture<Void>	move(final IPlan plan)
		{
			final Future<Void>	ret	= new Future<Void>();
			
			triescnt++;
			print("Trying "+move+" ("+triescnt+") ", depth);
			depth++;
			board.move(move);
					
			if(delay>0)
			{
				plan.waitFor(delay)
					.addResultListener(new DelegationResultListener<Void>(ret)
				{
					public void customResultAvailable(Void result)
					{
						IFuture<MoveGoal> fut = plan.dispatchSubgoal(new MoveGoal());
						fut.addResultListener(new ExceptionDelegationResultListener<MoveGoal, Void>(ret)
						{
							public void customResultAvailable(MoveGoal result)
							{
								ret.setResult(null);
							}
						});
					}
				});
			}
			else
			{
				IFuture<MoveGoal> fut = plan.dispatchSubgoal(new MoveGoal());
				fut.addResultListener(new ExceptionDelegationResultListener<MoveGoal, Void>(ret)
				{
					public void customResultAvailable(MoveGoal result)
					{
						ret.setResult(null);
					}
				});
			}
			
			return ret;
		}
		
		/**
		 *  The plan failure code.
		 */
		@PlanFailed
		public IFuture<Void> failed(IPlan plan)
		{
			assert board.getLastMove().equals(move): "Tries to takeback wrong move.";
			
			Future<Void>	ret	= new Future<Void>();
			
			depth--;
			print("Failed "+move, depth);
			board.takeback();
			if(delay>0)
			{
				plan.waitFor(delay).addResultListener(new DelegationResultListener<Void>(ret));
			}
			else
			{
				ret.setResult(null);
			}
			
			return ret;
		}

		/**
		 *  The plan passed code.
		 */
		@PlanPassed
		public void passed()
		{
			depth--;
			print("Passed "+move, depth);
		}

		/**
		 *  The plan passed code.
		 */
		@PlanAborted
		public void aborted(IGoal goal)
		{
			depth--;
			print((goal.isSucceeded()?"Succeeded ":"Aborted ")+move, depth);
		}

		@Override
		public String toString()
		{
			return "MovePlan("+move+")";
		}
	}


	/**
	 *  Print out an indented string.
	 *  @param text The text.
	 *  @param indent The number of cols to indent.
	 */
	protected void print(String text, int indent)
    {
		if(printsteps)
		{
			for(int x=0; x<indent; x++)
				System.out.print(" ");
			System.out.println(text);
		}
    }

	public static void main(String[] args)
	{
		IBDIAgent.create(new SokratesV3Agent());
	}
}
