package jadex.bding.impl;
import jadex.future.IFuture;
import java.util.Set;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.impl.RGoal.GoalState;
import jadex.core.IComponent;
import jadex.bding.IReasoner;
import jadex.bding.Intention;
import jadex.bding.Plan;

public class RIntention 
{
    protected RGoal goal;

    protected Intention intention;

    protected RPlan plan;

    protected PlanHistory history;

    protected IComponent component;

    public RIntention(Intention intention, IComponent component) 
    {   
        this.intention = intention;
        this.component = component;
    }

    public void execute()
    {
        IReasoner reasoner = getComponent().getFeature(IBDINGAgentFeature.class).getReasoner();

        //Set<Plan> plans = reasoner.generatePlans(this).get();
    
        //Plan plan = reasoner.selectPlan(this, plans).get();

        Plan plan = reasoner.generatePlan(goal).get();

        RPlan rplan = new RPlan(plan, getComponent());
        
        rplan.execute();
    }

    public RPlan getPlan() 
    {
        return plan;
    }

    public RIntention setPlan(RPlan plan) 
    {
        this.plan = plan;
        return this;
    }

    public Intention getIntention() 
    {
        return intention;
    }

    public void setIntention(Intention intention) 
    {
        this.intention = intention;
    }

    public PlanHistory getHistory() 
    {
        return history;
    }

    public void setHistory(PlanHistory history) 
    {
        this.history = history;
    }

    public IComponent getComponent() 
    {
        return component;
    }

    public void setComponent(IComponent component) 
    {
        this.component = component;
    }
}
