package jadex.bding.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jadex.bding.Plan;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class RPlan extends RIdElement
{
    protected Plan plan;

    protected Set<RGoal> subgoals = new HashSet<>();

    protected IComponent component;

    public RPlan(Plan plan, IComponent component) 
    {
        super("plan_"+plan.getName());
        this.plan = plan;
        this.component = component;
    }

    public IFuture<Void> execute()
    {
        Future<Void> ret = new Future<>();

        Map<String, Object> params = new HashMap<>();
        params.putAll(BeliefExtractor.extract(component));

        getPlan().getBody().execute(getComponent(), this, params).then(res ->
        {
            System.out.println("plan execution led to: "+res);

            // todo: update beliefs with results

            ret.setResult(null);

        }).catchEx(ret);

        return ret;
    }

    public Plan getPlan() 
    {
        return plan;
    }

    public IComponent getComponent() 
    {
        return component;
    }

    public void addSubgoal(RGoal goal)
    {
        subgoals.add(goal);
    }

    public void removeSubgoal(RGoal goal)
    {
        subgoals.remove(goal);
    }

    public Set<RGoal> getSubgoals()
    {
        return subgoals;
    }
    
    @Override
    public String toString() 
    {
        return "RPlan [id=" + id + ", plan=" + plan + "]";
    }
}
