package jadex.bding.impl.planbody;

import java.util.Map;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanStep;
import jadex.core.IComponent;
import jadex.future.IFuture;

public class SubGoalStep implements IPlanStep
{
    protected String goal;

    public SubGoalStep(String goal)
    {
        this.goal = goal;
    }

    @Override
    public IFuture<Map<String, Object>> execute(IComponent component, Map<String, Object> parameters)
    {
        IBDINGAgentFeature feature = component.getFeature(IBDINGAgentFeature.class);

        // Subgoal dispatchen und auf dessen completion warten.
        return null;
    }
}