package jadex.bding.impl;
import jadex.future.Future;
import jadex.future.IFuture;

import java.util.Map;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.bding.IReasoner;
import jadex.bding.Intention;

public class RIntention 
{
    protected RGoal goal;

    protected Intention intention;

    protected RPlan plan;

    protected PlanHistory history;

    public RIntention(Intention intention, RGoal goal) 
    {   
        this.intention = intention;
        this.goal = goal;
    }

    public IFuture<Void> execute()
    {
        Future<Void> ret = new Future<>();

        IComponent component = IComponentManager.get().getCurrentComponent();

        Map<String, Object> beliefsbefore = BeliefExtractor.extract(component);

        IReasoner reasoner = component.getFeature(IBDINGAgentFeature.class).getReasoner();

        //Set<Plan> plans = reasoner.generatePlans(this).get();
    
        //Plan plan = reasoner.selectPlan(this, plans).get();

        // Generate full plan with planbody
        reasoner.generatePlan(this, beliefsbefore).then(plan ->
        {
            if(plan==null)
            {
                ret.setException(new RuntimeException("No plan could be generated for intention"));
            }
            else
            {
                this.plan = new RPlan(plan, component);
                this.plan.execute().then(Void ->
                {
                    System.out.println("Plan executed");
                    Map<String, Object> beliefsafter = BeliefExtractor.extract(component);
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
        }).catchEx(ret);

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

    @Override
    public String toString() 
    {
        return "RIntention [intention=" + intention.getName() + "]";
    }
}
