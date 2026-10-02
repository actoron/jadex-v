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
import jadex.bding.Plan;

public class RIntention extends RIdElement
{
    protected RGoal goal;

    protected Intention intention;

    protected RPlan plan;

    protected PlanHistory history = new PlanHistory();

    public RIntention(Intention intention, RGoal goal)
    {
        super("intention_"+intention.getName());
        this.intention = intention;
        this.goal = goal;
    }

    public IFuture<Void> execute()
    {
        System.out.println("Intention execute() called: " + intention.getName());

        Future<Void> ret = new Future<>();

        IComponent component = IComponentManager.get().getCurrentComponent();
        IReasoner reasoner = component.getFeature(IBDINGAgentFeature.class).getReasoner();

        Map<String, Object> beliefs = BeliefExtractor.extract(component);

        reasoner.generateStrategicPlan(this, beliefs).then(splan ->
        {
            if(splan == null)
            {
                ret.setException(new RuntimeException("No strategic plan could be generated for intention"));
            }
            else
            {
                reasoner.operationalizeStrategicPlan(this, beliefs, splan).then(operationalPlan ->
                {
                    if(operationalPlan == null)
                    {
                        ret.setException(new RuntimeException("No operational plan could be generated for intention"));
                    }
                    else
                    {
                        Plan plan = new Plan(operationalPlan.getName(), operationalPlan.getDescription(), getIntention(), getIntention().getModel());
                        plan.setStrategicPlan(splan);
                        executePlan(plan).delegateTo(ret);
                    }
                }).catchEx(ret);
            }
        }).catchEx(ret);

        return ret;
    }

    protected IFuture<Void> executePlan(Plan plan)
    {
        Future<Void> ret = new Future<>();

        IComponent component = IComponentManager.get().getCurrentComponent();

        IReasoner reasoner = component.getFeature(IBDINGAgentFeature.class).getReasoner();

        this.plan = new RPlan(plan, this, component);

        this.plan.execute().then(Void ->
        {
            Map<String, Object> beliefsafter = BeliefExtractor.extract(component);

            reasoner.isIntentionAchieved(this, beliefsafter).then(achieved ->
            {
                if(achieved)
                {
                    ret.setResult(null);
                }
                else
                {
                    reasoner.isRetryAllowed(this.plan, beliefsafter).then(retry ->
                    {
                        if(retry)
                        {
                            System.out.println("retry plan: "+plan);

                            retryPlan().delegateTo(ret);
                        }
                        else
                        {
                            System.out.println("replan");

                            replan().delegateTo(ret);
                        }
                    }).catchEx(ret).printOnEx();
                }
            }).catchEx(ret).printOnEx();

        }).catchEx(ex ->
        {
            ex.printStackTrace();
            replan().delegateTo(ret);
        });

        return ret;
    }

    protected IFuture<Void> retryPlan()
    {
        history.addEntry(new PlanHistoryEntry(plan));

        return executePlan(plan.getPlan());
    }

    protected IFuture<Void> replan()
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
        if(this == obj)
            return true;

        if(obj == null)
            return false;

        if(getClass() != obj.getClass())
            return false;

        RIntention other = (RIntention)obj;

        if(id == null)
        {
            if(other.id != null)
                return false;
        }
        else if(!id.equals(other.id))
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "RIntention [intention="+intention.getName()+"]";
    }
}