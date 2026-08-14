package jadex.bding.impl;
import jadex.future.Future;
import jadex.future.IFuture;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
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

    public RIntention(Intention intention, RGoal goal, IComponent component) 
    {   
        this.intention = intention;
        this.goal = goal;
        this.component = component;
    }

    public IFuture<Void> execute()
    {
        Future<Void> ret = new Future<>();

        BeliefSnapshot beliefsbefore = BeliefSnapshot.extract(component);

        IReasoner reasoner = getComponent().getFeature(IBDINGAgentFeature.class).getReasoner();

        //Set<Plan> plans = reasoner.generatePlans(this).get();
    
        //Plan plan = reasoner.selectPlan(this, plans).get();

        // Generate full plan with planbody
        Plan plan = reasoner.generatePlan(this, beliefsbefore).get();

        if(plan==null)
        {
            ret.setException(new RuntimeException("No plan could be generated for intention"));
        }
        else
        {
            this.plan = new RPlan(plan, getComponent());
            this.plan.execute().then(Void ->
            {
                System.out.println("Plan executed");
                BeliefSnapshot beliefsafter = BeliefSnapshot.extract(component);
                reasoner.isIntentionAchieved(this, beliefsafter).then(state ->
                {
                    if(state)
                    {
                        ret.setResult(null);
                    }
                    else
                    {
                        tryNextPlan().delegateTo(ret);
                    }
                });
                  
            }
            ).catchEx(ex ->
            {
                tryNextPlan().delegateTo(ret);
            });
        }

        return ret;
    }

    protected IFuture<Void> tryNextPlan()
    {
        history.addEntry(new PlanHistoryEntry(plan));

        return execute();
    }

    public RGoal getGoal() 
    {
        return goal;
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
