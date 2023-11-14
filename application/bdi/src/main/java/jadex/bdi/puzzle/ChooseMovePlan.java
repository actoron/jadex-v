package jadex.bdi.puzzle;

import java.util.ArrayList;
import java.util.Collection;

import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.puzzle.SokratesAgent.ChooseMoveGoal;
import jadex.bdi.runtime.impl.ICandidateInfo;

/**
 *  Meta-level reasoning plan for choosing between applicable plans.
 */
@Plan
public class ChooseMovePlan
{
	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body(SokratesAgent agent, ChooseMoveGoal goal)
	{
		switch(agent.ml)
		{
			case SHORT:
				goal.cand	= selectPlan(goal.cands, agent.board, true, false, false, true); break;
			case LONG:
				goal.cand	= selectPlan(goal.cands, agent.board, true, true, false, true); break;
			case SAME_LONG:
				goal.cand	= selectPlan(goal.cands, agent.board, true, true, true, true); break;
			case ALTER_LONG:
				goal.cand	= selectPlan(goal.cands, agent.board, false, true, true, true); break;
			case NONE:
			default:
				goal.cand	= goal.cands.iterator().next(); break;
		}
	}

	/**
	 *  Select a move with respect to color resp. move kind (jump vs. normal).
	 *  @param plans The list of applicable plans distinguished by move field.
	 *  @param board The board.
	 *  @param same Prefer moves of same color.
	 *  @param jump Prefer jump moves.
	 *  @param consider_color Consider the color.
	 *  @param consider_jump Consider the move kind.
	 */
	protected ICandidateInfo selectPlan(Collection<ICandidateInfo> plans, IBoard board, boolean same, boolean jump,
		boolean consider_color, boolean consider_jump)
	{
		Collection<ICandidateInfo> sel_col = new ArrayList<>();
		if(consider_color)
		{
			for(ICandidateInfo plan: plans)
			{
				if(matchColor(board, ((MovePlan)plan.getRawCandidate()).move, same))
				{
					sel_col.add(plan);
				}
			}
		}
		else
		{
			sel_col = plans;
		}

		Collection<ICandidateInfo> sel_jump = new ArrayList<>();
		if(consider_jump)
		{
			for(int i=0; i<sel_col.size(); i++)
			{
				ICandidateInfo	plan	= sel_col.iterator().next();
				if(matchJump(board, ((MovePlan)plan.getRawCandidate()).move, jump))
				{
					sel_jump.add(plan);
				}
			}
		}
		else
		{
			sel_jump = sel_col;
		}

		assert sel_col.size()>0 || sel_jump.size()>0 || plans.size()>0;

		ICandidateInfo ret = null;
		if(sel_jump.size()>0)
			ret = sel_jump.iterator().next();
		else if(sel_col.size()>0)
			ret = sel_col.iterator().next();
		else
			ret = plans.iterator().next();

		return ret;
	}

	/**
	 *  Match move with color constraint.
	 */
	protected boolean matchColor(IBoard board, Move move, boolean prefer_samecolor)
	{
		Piece piece = board.getPiece(move.getStart());
		if(piece==null)
			throw new RuntimeException("Impossible move: "+move);
		boolean same = board.wasLastMoveWhite()==board.getPiece(move.getStart()).isWhite();
		return prefer_samecolor==same;
	}

	/**
	 *  Match move with jump constraint.
	 */
	protected boolean matchJump(IBoard board, Move move, boolean prefer_jump)
	{
		return prefer_jump==move.isJumpMove();
	}
}
