package jadex.bding;

import jadex.future.IFuture;
import java.util.Set;

import jadex.bding.impl.RGoal;

public interface IReasoner 
{
    public IFuture<Set<Intention>> generateIntentions(RGoal goal);

    public IFuture<Intention> selectIntention(RGoal goal, Set<Intention> intentions);

    public IFuture<Plan> generatePlan(RGoal goal);

    //public IFuture<Set<Plan>> generatePlans(RIntention intention);

    //public IFuture<Plan> selectPlan(RGoal goal, Set<Plan> plans);

    /**
     * Helper method to compare two strings for semantic equality
     */
    public IFuture<Boolean> equals(String str1, String str2);
}
