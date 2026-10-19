package jadex.bding.impl;

import java.util.Map;

import jadex.future.Future;
import jadex.future.IFuture;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IReasoner;
import jadex.bding.Intention;
import jadex.bding.Plan;
import jadex.bding.impl.PlanHistory.PlanHistoryEntry;
import jadex.common.transformation.traverser.SCloner;
import jadex.core.IComponent;
import jadex.core.IComponentManager;

public class RIntention extends RIdElement
{
    protected enum FailureAction
    {
        RETRY,
        REPLAN,
        FAIL
    }

    protected static final int MAX_RETRIES = 2;
    protected static final int MAX_REPLANS = 3;
    protected static final int PROGRESS_CHECK_INTERVAL = 3;

    protected RGoal goal;
    protected Intention intention;
    protected RPlan plan;
    protected PlanHistory history = new PlanHistory();

    protected int retryCount;
    protected int replanCount;
    protected int continueCount;

    protected Map<String, Object> lastcontext;

    public RIntention()
    {
    }

    public RIntention(Intention intention, RGoal goal)
    {
        super("intention_" + intention.getName());
        this.intention = intention;
        this.goal = goal;
    }

    public IFuture<Void> execute()
    {
        System.out.println("Intention execute() called: " + intention.getName());

        Future<Void> ret = new Future<>();
        IComponent component = IComponentManager.get().getCurrentComponent();
        IReasoner reasoner = component.getFeature(IBDINGAgentFeature.class).getReasoner();
        Map<String, Object> context = RIdElement.createContext(component, getGoal());

        reasoner.generateStrategicPlan(this, context).then(splan ->
        {
            if(splan == null)
            {
                ret.setException(new RuntimeException("No strategic plan could be generated for intention"));
                return;
            }

            reasoner.createPlanDataFlow(this, context, splan).then(operationalPlan ->
            {
                if(operationalPlan == null)
                {
                    ret.setException(new RuntimeException("No operational plan could be generated for intention"));
                    return;
                }

                Plan newPlan = new Plan(operationalPlan.getName(), operationalPlan.getDescription(), getIntention(), getIntention().getModel());
                newPlan.setStrategicPlan(operationalPlan);

                executePlan(newPlan).delegateTo(ret);
            }).catchEx(ret);
        }).catchEx(ret);

        return ret;
    }

    protected IFuture<Void> executePlan(Plan executedPlan)
    {
        Future<Void> ret = new Future<>();
        IComponent component = IComponentManager.get().getCurrentComponent();

        this.plan = new RPlan(executedPlan, this, component);

        this.plan.execute().then(result ->
        {
            handlePlanResult(executedPlan).delegateTo(ret);
        }).catchEx(ex ->
        {
            handleExecutionFailure(ex).delegateTo(ret);
        });

        return ret;
    }

    protected IFuture<Void> handlePlanResult(Plan plan)
    {
        Future<Void> ret = new Future<>();
        IComponent component = IComponentManager.get().getCurrentComponent();
        IReasoner reasoner = component.getFeature(IBDINGAgentFeature.class).getReasoner();
        Map<String, Object> context = RIdElement.createContext(component, getGoal());

        reasoner.isIntentionAchieved(this, context).then(achieved ->
        {
            if(achieved)
            {
                ret.setResult(null);
            }
            else
            {
                continuePlan(plan, context, reasoner).delegateTo(ret);
            }
        }).catchEx(ret);

        return ret;
    }

    protected IFuture<Void> continuePlan(Plan executedPlan, Map<String, Object> context, IReasoner reasoner)
    {
        Future<Void> ret = new Future<>();

        if(lastcontext == null)
            lastcontext = (Map<String, Object>)SCloner.clone(context);

        if(++continueCount < PROGRESS_CHECK_INTERVAL)
        {
            executePlan(executedPlan).delegateTo(ret);
            return ret;
        }

        continueCount = 0;

        Map<String, Object> currentContext = RIdElement.createContext(IComponentManager.get().getCurrentComponent(), getGoal());
        Map<String, Object> previousContext = lastcontext;

        reasoner.hasMadeProgress(this, previousContext, currentContext).then(progress ->
        {
            lastcontext = (Map<String, Object>)SCloner.clone(context);

            if(progress)
            {
                executePlan(executedPlan).delegateTo(ret);
            }
            else
            {
                System.out.println("No progress detected, replanning");
                replan().delegateTo(ret);
            }
        }).catchEx(ret);

        return ret;
    }

    protected IFuture<Void> handleExecutionFailure(Exception ex)
    {
        FailureAction type = classifyFailure(ex);
        System.err.println("Plan execution failed: " + ex.getMessage()+" "+type);

        switch(type)
        {
            case RETRY:
                if(retryCount < MAX_RETRIES)
                    return retryPlan();
                else
                    return replan();

            case REPLAN:
                return replan();

            case FAIL:
                Future<Void> ret = new Future<>();
                ret.setException(ex);
                return ret;

            default:
                Future<Void> error = new Future<>();
                error.setException(new IllegalStateException("Unknown plan failure classification", ex));
                return error;
        }
    }

    /*protected FailureAction classifyFailure(Exception ex)
    {
        if(ex instanceof TransientPlanException)
            return FailureAction.RETRY;

        if(ex instanceof InvalidPlanException)
            return FailureAction.REPLAN;

        if(ex instanceof UnrecoverablePlanException)
            return FailureAction.FAIL;

        // Unknown failures are not retried indefinitely.
        return FailureAction.REPLAN;
    }*/

    protected FailureAction classifyFailure(Exception ex)
    {
        // 1. Explicit cancellation or unrecoverable errors.
        if(ex instanceof java.util.concurrent.CancellationException
            || ex instanceof UnsupportedOperationException
            || ex instanceof IllegalArgumentException
            || ex instanceof SecurityException)
        {
            return FailureAction.FAIL;
        }

        // 2. Likely programming, validation or compilation errors.
        if(ex instanceof NullPointerException
            || ex instanceof ClassCastException
            || ex instanceof IndexOutOfBoundsException
            || ex instanceof IllegalStateException)
        {
            String message = ex.getMessage();
            String lower = message != null ? message.toLowerCase(java.util.Locale.ROOT) : "";

            if(lower.contains("compile") || lower.contains("validation")
                || lower.contains("invalid plan") || lower.contains("unresolved")
                || lower.contains("input not available") || lower.contains("not compiled"))
            {
                return FailureAction.REPLAN;
            }

            return FailureAction.FAIL;
        }

        // 3. Common transient failures.
        if(ex instanceof java.util.concurrent.TimeoutException || ex instanceof java.io.IOException)
        {
            return FailureAction.RETRY;
        }

        // 4. Coarse classification for ordinary RuntimeExceptions.
        if(ex instanceof RuntimeException)
        {
            String message = ex.getMessage();
            String lower = message != null ? message.toLowerCase(java.util.Locale.ROOT) : "";

            if(containsAny(lower, "timeout", "timed out", "temporarily unavailable",
                "connection reset", "connection refused", "connection closed",
                "broken pipe", "rate limit", "too many requests", "429",
                "service unavailable", "503", "502", "504", "try again"))
            {
                return FailureAction.RETRY;
            }

            if(containsAny(lower, "compile", "compilation", "validation",
                "invalid plan", "unresolved", "input not available",
                "not compiled", "missing tool", "unknown tool",
                "unknown action", "unsupported operation"))
            {
                return FailureAction.REPLAN;
            }

            if(containsAny(lower, "out of memory", "stack overflow",
                "cancelled", "canceled", "permission denied", "access denied"))
            {
                return FailureAction.FAIL;
            }

            // Unknown RuntimeExceptions: try generating a different plan.
            return FailureAction.REPLAN;
        }

        // 5. Other checked exceptions: retry only if their type is known to be transient.
        return FailureAction.REPLAN;
    }

    protected boolean containsAny(String text, String... fragments)
    {
        for(String fragment : fragments)
        {
            if(text.contains(fragment))
                return true;
        }

        return false;
    }

    protected IFuture<Void> retryPlan()
    {
        history.addEntry(new PlanHistoryEntry(plan));
        retryCount++;

        System.out.println("Retry same plan (" + retryCount + "/" + MAX_RETRIES + "): " + plan);

        return executePlan(plan.getPlan());
    }

    protected IFuture<Void> replan()
    {
        if(plan != null)
            history.addEntry(new PlanHistoryEntry(plan));

        if(replanCount >= MAX_REPLANS)
        {
            Future<Void> ret = new Future<>();
            ret.setException(new RuntimeException("Maximum replans exceeded for intention: " + intention.getName()));
            return ret;
        }

        replanCount++;
        retryCount = 0;
        continueCount = 0;

        System.out.println("Replan (" + replanCount + "/" + MAX_REPLANS + ")");

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
        return "RIntention [intention=" + intention.getName() + "]";
    }

}
