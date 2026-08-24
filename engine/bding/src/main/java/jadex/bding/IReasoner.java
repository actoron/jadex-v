package jadex.bding;

import jadex.future.IFuture;

import java.util.List;
import java.util.Set;

import jadex.bding.impl.BeliefSnapshot;
import jadex.bding.impl.RGoal;
import jadex.bding.impl.RIntention;
import jadex.bding.impl.RGoal.GoalState;

/**
 * Cognitive reasoning operations:
 *
 * generateIntentions   What could I do?
 * selectIntention      What do I want to do from these options?
 * generatePlan         How do I realize this intention?
 * isSameIntention      Have I already pursued this intention?
 * evaluateGoalState    Has my goal been achieved or failed?
 */
public interface IReasoner 
{
    public IFuture<RGoal> createGoal(String usergoal, AgentModel model);

    public IFuture<Set<Intention>> generateIntentions(RGoal goal, BeliefSnapshot beliefs);

    public IFuture<Intention> selectIntention(RGoal goal, Set<Intention> intentions, BeliefSnapshot beliefs);

    public IFuture<Plan> generatePlan(RIntention intention, BeliefSnapshot beliefs);

    //public IFuture<Set<Plan>> generatePlans(RIntention intention);

    //public IFuture<Plan> selectPlan(RGoal goal, Set<Plan> plans);

    public IFuture<Boolean> isIntentionAchieved(RIntention in, BeliefSnapshot beliefs);

    public IFuture<Boolean> isSameIntention(Intention in1, Intention in2);

    public IFuture<GoalState> evaluateGoalState(RGoal goal, BeliefSnapshot beliefs);

    public IFuture<ReasoningEntry> getCurrentReasoning();

    public IFuture<List<ReasoningEntry>> getReasoningHistory();
}
