package jadex.bding.impl.planbody;

import java.util.Map;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanStep;
import jadex.bding.impl.RIdElement;
import jadex.bding.impl.RPlan;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class SubgoalStep extends RIdElement implements IPlanStep
{
    protected String goal;

    public SubgoalStep(String goal)
    {
        super("subgoalstep");
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
 
    public String getId() 
    {
        return id;
    }

    @Override
    public int hashCode() 
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) 
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SubgoalStep other = (SubgoalStep) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}