package jadex.bding.impl.planbody;

import java.util.Map;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanStep;
import jadex.bding.impl.RPlan;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class SubgoalStep implements IPlanStep
{
    protected String goal;

    public SubgoalStep(String goal)
    {
        this.goal = goal;
    }

    @Override
    public IFuture<Map<String, Object>> execute(IComponent component, Map<String, Object> parameters, RPlan plan)
    {
        IBDINGAgentFeature feature = component.getFeature(IBDINGAgentFeature.class);

        Future<Map<String, Object>> ret = new Future<>();

        try
        {
            feature.dispatchSubgoal(goal, plan).then(subgoal ->
            {
                subgoal.getFinished().then(result ->
                {
                    ret.setResult(parameters);
                }).catchEx(ex ->
                {
                    ret.setException(ex);
                });
            }).catchEx(ex ->
            {
                ret.setException(ex);
            });
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }

    public String getGoal() 
    {
        return goal;
    }
    
}