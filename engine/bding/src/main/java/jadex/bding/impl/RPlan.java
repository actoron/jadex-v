package jadex.bding.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jadex.bding.AgentModel;
import jadex.bding.Belief;
import jadex.bding.Plan;
import jadex.bding.impl.planbody.IncrementalPlanBody;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class RPlan extends RIdElement
{
    protected Plan plan;

    protected RIntention intention; //parent intention

    protected Set<RGoal> subgoals = new HashSet<>();

    protected IComponent agent;

    public RPlan(Plan plan, RIntention intention, IComponent agent) 
    {
        super("plan_"+plan.getName());
        this.plan = plan;
        this.intention = intention;
        this.agent = agent;
    }

    public IFuture<Void> execute()
    {
        Future<Void> ret = new Future<>();

        // prepare the context map with beliefs and goal parameter
        Map<String, Object> params = createContext(getAgent(), intention.getGoal());

        if(getPlan().getBody()!=null)
        {
            getPlan().getBody().execute(getAgent(), this, params)
            .then(res ->
            {
                System.out.println("plan execution led to: "+res);

                ret.setResult(null);

            }).catchEx(ret);
        }
        else if(getPlan().getStrategicPlan()!=null)
        {
            IncrementalPlanBody ibody = new IncrementalPlanBody(getPlan().getStrategicPlan());
            ibody.execute(getAgent(), this, params)
            .then(res ->
            {
                System.out.println("plan execution led to: "+res);

                ret.setResult(null);

            }).catchEx(ret);
        }

        return ret;
    }

    public Plan getPlan() 
    {
        return plan;
    }

    public IComponent getAgent() 
    {
        return agent;
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

    public void setPlan(Plan plan) 
    {
        this.plan = plan;
    }

    public void setSubgoals(Set<RGoal> subgoals) 
    {
        this.subgoals = subgoals;
    }

    public void setAgent(IComponent component) 
    {
        this.agent = component;
    }

    public RIntention getIntention() 
    {
        return intention;
    }

    public void setIntention(RIntention intention) 
    {
        this.intention = intention;
    }

    public static Map<String, Object> createContext(IComponent agent, RGoal goal)
    {
        Map<String, Object> params = new HashMap<>();
        
        Map<String, Object> bels = BeliefExtractor.extract(agent);
        for(Entry<String, Object> b: bels.entrySet())
        {
            params.put("belief."+b.getKey(), b.getValue());
        }
        
        Map<String, Object> goalparams = goal.getParameters();
        for(Entry<String, Object> gp: goalparams.entrySet())
        {
            params.put("goal."+gp.getKey(), gp.getValue());
        }

        return params;
    }
}
