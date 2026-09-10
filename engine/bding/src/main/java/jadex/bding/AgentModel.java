package jadex.bding;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentModel 
{
    protected Map<String, Goal> goals = new LinkedHashMap<>();

    protected Map<String, Intention> intentions = new LinkedHashMap<>();
    
    protected Map<String, Plan> plans = new LinkedHashMap<>();

    protected Map<String, Belief> beliefs = new LinkedHashMap<>();

    public Map<String, Goal> getGoals() 
    {
        return goals;
    }

    public void setGoals(Map<String, Goal> goals) 
    {
        this.goals = goals;
    }

    public AgentModel addGoal(Goal goal)
    {
        goals.put(goal.getName(), goal);
        return this;
    }

    public Goal getGoal(String name)
    {
        return goals.get(name);
    }

    public Map<String, Intention> getIntentions() 
    {
        return intentions;
    }

    public void setIntentions(Map<String, Intention> intentions) 
    {
        this.intentions = intentions;
    }

    public AgentModel addIntention(Intention intention)
    {
        intentions.put(intention.getName(), intention);
        return this;
    }

    public Map<String, Plan> getPlans() 
    {
        return plans;
    }

    public void setPlans(Map<String, Plan> plans) 
    {
        this.plans = plans;
    }

     public AgentModel addPlan(Plan plan)
    {
        plans.put(plan.getName(), plan);
        return this;
    }

    public Map<String, Belief> getBeliefs() 
    {
        return beliefs;
    }

    public void setBeliefs(Map<String, Belief> beliefs) 
    {
        this.beliefs = beliefs;
    }

    public AgentModel addBelief(Belief belief)
    {
        beliefs.put(belief.getName(), belief);
        return this;
    }

    public Belief getBelief(String name)
    {
        return beliefs.get(name);
    }
    
}
