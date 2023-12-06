package jadex.bdi.puzzle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.puzzle.SokratesMLRAgent.ChooseMoveGoal;
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
	public void body(SokratesMLRAgent agent, ChooseMoveGoal goal)
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
	protected ICandidateInfo selectPlan(List<ICandidateInfo> plans, IBoard board, boolean same, boolean jump,
		boolean consider_color, boolean consider_jump)
	{
		List<ICandidateInfo> sel_col = new ArrayList<>();
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
		
		if(sel_col.isEmpty())
		{
			sel_col = plans;
		}

		List<ICandidateInfo> sel_jump = new ArrayList<>();
		if(consider_jump)
		{
			for(ICandidateInfo plan: sel_col)
			{
				if(matchJump(board, ((MovePlan)plan.getRawCandidate()).move, jump))
				{
					sel_jump.add(plan);
				}
			}
		}

		if(sel_jump.isEmpty())
		{
			sel_jump = sel_col;
		}

		return sel_jump.get(0);
	}

	/**
	 *  Match move with color constraint.
	 */
	protected boolean matchColor(IBoard board, Move move, boolean prefer_samecolor)
	{
		Piece piece = board.getPiece(move.getStart());
		if(piece==null)
			throw new RuntimeException("Impossible move: "+move);
		boolean same = board.wasLastMoveWhite()==piece.isWhite();
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
