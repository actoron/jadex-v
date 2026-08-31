package jadex.bding;

import java.util.Map;
import java.util.Set;

import jadex.bding.impl.RGoal;
import jadex.bding.impl.RPlan;
import jadex.core.IComponentFeature;
import jadex.future.IFuture;

/**
 *  Public methods for working with BDI agents.
 */
public interface IBDINGAgentFeature	extends IComponentFeature
{
    public IReasoner getReasoner();

    public AgentModel getModel();

    public Set<RGoal> getGoals();

    public Map<String, Object> getBeliefs();

    public IFuture<RGoal> dispatchTopLevelGoal(String usergoal);

    public IFuture<RGoal> dispatchSubgoal(String usergoal, RPlan plan);

    //public ITerminableFuture<Void> dispatchTopLevelGoal(Goal goal);
}
