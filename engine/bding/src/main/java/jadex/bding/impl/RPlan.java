package jadex.bding.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jadex.bding.Belief;
import jadex.bding.IBDINGAgentFeature;
import jadex.bding.Plan;
import jadex.bding.impl.planbody.PlanExecutionContext;
import jadex.bding.impl.planbody.PlanStepExecution;
import jadex.collection.PathMap;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class RPlan extends RIdElement
{
    protected Plan plan;

    protected RIntention intention; //parent intention

    protected Set<RGoal> subgoals = new HashSet<>();

    protected List<PlanStepExecution> executedSteps = new ArrayList<>();

    protected IComponent agent;

    // Bean constructor
    public RPlan()
    {
    }

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

        Map<String, Object> params = createContext(getAgent(), intention.getGoal());
        
        PlanExecutionContext context = new PlanExecutionContext(this, params);

        if(getPlan().getBody() != null)
        {
            getPlan().getBody().execute(getAgent(), context).then(res ->
            {
                System.out.println("plan execution led to: " + res);

                writeBackContext(context, intention.getGoal(), getAgent());
                ret.setResult(null);
            })
            .catchEx(ret);
        }
        else if(getPlan().getStrategicPlan() != null)
        {

            getPlan().getStrategicPlan().getExecutableStep(agent.getFeature(IBDINGAgentFeature.class).getReasoner(), 
                this, context.getParameters()).then(step ->
            {
                step.execute(getAgent(), context)
                .then(res ->
                {
                    System.out.println("plan execution led to: " + res);

                    writeBackContext(context, intention.getGoal(), getAgent());
                    ret.setResult(null);
                })
                .catchEx(ret);
            }).catchEx(ret);
        }
        else
        {
            ret.setException(new RuntimeException("Plan '" + getPlan().getName()+ "' has neither a body nor a strategic plan."));
        }

        return ret;
    }

    public Plan getPlan() 
    {
        return plan;
    }

    public void setPlan(Plan plan) 
    {
        this.plan = plan;
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

    public List<PlanStepExecution> getExecutedSteps() 
    {
        return executedSteps;
    }

    public void setExecutedSteps(List<PlanStepExecution> executedSteps) 
    {
        this.executedSteps = executedSteps;
    }

    public void addExecutedStep(PlanStepExecution exe)
    {
        executedSteps.add(exe);
    }

    public static Map<String, Object> createContext(IComponent agent, RGoal goal)
    {
        Map<String, Object> params = new PathMap();

        for(String name: goal.getParameters().keySet())
        {
            params.put("goal."+name, goal.getParameters().get(name));
        }

        Map<String, Object> bels = BeliefExtractor.extract(agent);
        for(String name: bels.keySet())
        {
            params.put("belief."+name, bels.get(name));
        }

        return params;
    }

    public static void writeBackContext(PlanExecutionContext context, RGoal goal, IComponent agent)
    {
        // Only write back dirty values
        for(String key : context.getDirty())
        {
            Object value = context.get(key);

            if(key.startsWith("belief."))
            {
                String name = key.substring("belief.".length());

                Belief bel = goal.getGoal().getModel().getBeliefs().get(name);

                if(bel == null)
                {
                    System.out.println("belief not found: " + name);
                }
                else
                {
                    bel.setValue(agent.getPojo(), value);
                    System.out.println("Updated belief: " + name + " " + value);
                }
            }
            else if(key.startsWith("goal."))
            {
                String name = key.substring("goal.".length());

                goal.getParameters().put(name, value);
                System.out.println("Updated goal parameter: " + name + " " + value);
            }
        }
    }

}
