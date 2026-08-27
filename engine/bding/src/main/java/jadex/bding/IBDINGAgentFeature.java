package jadex.bding;

import java.util.Set;

import jadex.bding.impl.BeliefSnapshot;
import jadex.bding.impl.RGoal;
import jadex.core.IComponentFeature;
import jadex.future.ITerminableFuture;

/**
 *  Public methods for working with BDI agents.
 */
public interface IBDINGAgentFeature	extends IComponentFeature
{
    public IReasoner getReasoner();

    public AgentModel getModel();

    public Set<RGoal> getGoals();

    public BeliefSnapshot getBeliefs();

    public ITerminableFuture<Void> dispatchTopLevelGoal(String usergoal);

    //public ITerminableFuture<Void> dispatchTopLevelGoal(Goal goal);
}
