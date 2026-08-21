package jadex.bding;

import jadex.core.IComponentFeature;
import jadex.future.ITerminableFuture;

/**
 *  Public methods for working with BDI agents.
 */
public interface IBDINGAgentFeature	extends IComponentFeature
{
    public IReasoner getReasoner();

    public AgentModel getModel();

    public ITerminableFuture<Void> dispatchTopLevelGoal(String usergoal);

    //public ITerminableFuture<Void> dispatchTopLevelGoal(Goal goal);
}
