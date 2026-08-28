package jadex.bding;

import jadex.future.IFuture;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public enum ReasoningType
    {
        BOOLEAN, // true/false answer
        SELECTION, // number/int answer
        COMPUTATION, // number/double answer
        EXPLANATION // textual answer
    }

    public IFuture<RGoal> createGoal(String usergoal, AgentModel model, Map<String, Object> context);

    public IFuture<Set<Intention>> generateIntentions(RGoal goal, Map<String, Object> context);

    public IFuture<Intention> selectIntention(RGoal goal, Set<Intention> intentions, Map<String, Object> context);

    public IFuture<Plan> generatePlan(RIntention intention, Map<String, Object> context);

    //public IFuture<Set<Plan>> generatePlans(RIntention intention);

    //public IFuture<Plan> selectPlan(RGoal goal, Set<Plan> plans);

    public IFuture<Boolean> isIntentionAchieved(RIntention in, Map<String, Object> context);

    public IFuture<Boolean> isSameIntention(Intention in1, Intention in2);

    public IFuture<GoalState> evaluateGoalState(RGoal goal, Map<String, Object> context);

    public IFuture<Set<ReasoningEntry>> getCurrentReasoning();

    public IFuture<List<ReasoningEntry>> getReasoningHistory();

    public IFuture<Object> reason(String problem, AgentModel model, Map<String, Object> context, ReasoningType type);
}
