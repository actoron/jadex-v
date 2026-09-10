package jadex.bding.tool;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IReasoner;
import jadex.bding.ReasoningEntry;
import jadex.bding.impl.RGoal;
import jadex.core.IComponentHandle;
import jadex.core.INoCopyStep;

public class BDIInspector
{
    protected final IComponentHandle agent;

    public BDIInspector(IComponentHandle agent)
    {
        this.agent = agent;
    }

    public BDISnapshot createSnapshot()
    {
        return agent.scheduleStep((INoCopyStep<BDISnapshot>)ag ->
        {
            IBDINGAgentFeature feature = ag.getFeature(IBDINGAgentFeature.class);

            Set<RGoal> goals = feature.getGoals();
            Map<String, Object> beliefs = feature.getBeliefs();

            IReasoner reasoner = feature.getReasoner();

            Set<ReasoningEntry> current = reasoner.getCurrentReasoning().get();

            List<ReasoningEntry> history = reasoner.getReasoningHistory().get();

            return new BDISnapshot(goals, beliefs, current, history);
        }).get();
    }
}