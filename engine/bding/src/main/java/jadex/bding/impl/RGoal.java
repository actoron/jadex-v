package jadex.bding.impl;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IReasoner;
import jadex.bding.Intention;
import jadex.bding.impl.IntentionHistory.IntentionHistoryEntry;
import jadex.bding.Goal;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.future.Future;
import jadex.future.IFuture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RGoal extends RIdElement
{
    public enum GoalState
	{
		INACTIVE,
		ACTIVE,
		SUCCEEDED,
		FAILED,
		DROPPED
	}

    protected Goal goal;

    protected Map<String, Object> parameters = new LinkedHashMap<>();

    protected RIntention intention;

    protected GoalState state = GoalState.INACTIVE;

    protected IntentionHistory history = new IntentionHistory();

    /** The finished future (if someone waits for the goal). */
	protected TerminableFuture<Object>	finished;

    protected Exception exception;

    protected RPlan parentPlan;

    public RGoal(Goal goal, Map<String, Object> parameters)
    {
        super("goal_"+goal.getName());
        this.goal = goal;
        this.parameters = parameters;
    }

    public Goal getGoal() 
    {
        return goal;
    }

    public RGoal setIntention(RIntention intention) 
    {
        this.intention = intention;
        return this;
    }

    public RIntention getIntention() 
    {
        return intention;
    }

    public void adopt()
    {
        IComponent agent = IComponentManager.get().getCurrentComponent();
        BDINGAgentFeature bdif = (BDINGAgentFeature)agent.getFeature(IBDINGAgentFeature.class);
        bdif.addGoal(this);
        execute();
    }

    public IFuture<Void> execute()
    {
        Future<Void> ret = new Future<>();

        boolean igen = false;

        try
        {
            setState(GoalState.ACTIVE);

            IComponent component = IComponentManager.get().getCurrentComponent();
            IReasoner reasoner = component.getFeature(IBDINGAgentFeature.class).getReasoner();

            while(true)
            {
                Map<String, Object> beliefs = BeliefExtractor.extract(component);

                Set<Intention> intentions = getGoal().getIntentions();

                if(!igen)
                {
                    igen = true;
                    intentions = reasoner.generateIntentions(this, beliefs).get();
                    getGoal().setIntentions(intentions);
                }

                Set<Intention> possible = getGoal().getIntentions().stream()
                    .filter(intention -> !history.isKnown(intention))
                    .collect(Collectors.toSet());

                if(possible.isEmpty())
                {
                    if(evaluateGoalState().get()==GoalState.SUCCEEDED)
                        setState(GoalState.SUCCEEDED);
                    else
                        setState(GoalState.FAILED);

                    break;
                }
                else
                {
                    System.out.println("Possible: "+possible.size()+" "+possible);
                }

                Intention intention = possible.size() == 1
                    ? possible.iterator().next()
                    : reasoner.selectIntention(this, possible, beliefs).get();

                RIntention rintention = new RIntention(intention, this);
                setIntention(rintention);

                System.out.println("---- GOAL LOOP ----");
                System.out.println("History intentions: " + getHistory().getEntries());
                System.out.println("All intentions: " + getGoal().getIntentions());
                System.out.println("Possible intentions: " + possible);
                System.out.println("Selected intention: " + getIntention());

                try
                {
                    rintention.execute().get();

                    if(evaluateGoalState().get()==GoalState.SUCCEEDED)
                    {
                        setState(GoalState.SUCCEEDED);
                        break;
                    }

                    history.addEntry(new IntentionHistoryEntry(rintention));
                }
                catch(Exception e)
                {
                    history.addEntry(new IntentionHistoryEntry(rintention));
                }
            }

            ret.setResult(null);
        }
        catch(Exception e)
        {
            this.exception = e;
            System.out.println("Goal failed: "+e.getMessage());
            ret.setExceptionIfUndone(e);
            setState(GoalState.FAILED);
        }

        return ret;
    }

    public void drop()
    {
        throw new UnsupportedOperationException();
        //changeState(GoalState.DROPPED);
    }

    public GoalState getState() 
    {
        return state;
    }

    public void setState(GoalState newstate)
    {
        if(state == newstate)
            throw new IllegalStateException("Goal is already in state " + state);

        if(isFinished(state))
            throw new IllegalStateException("Cannot change finished goal from " + state + " to " + newstate);

        if(newstate == GoalState.ACTIVE && state != GoalState.INACTIVE)
            throw new IllegalStateException("Goal can only become ACTIVE from INACTIVE, but is " + state);

        state = newstate;

        if(isFinished(newstate))
        {
            if(newstate == GoalState.SUCCEEDED)
                ((TerminableFuture<Void>)getFinished()).setResult(null);
            else
                ((TerminableFuture<Void>)getFinished()).setException(exception != null ? exception: new RuntimeException("Goal " + newstate));
        }
    }

    /**
	 *  Get the finished future to wait for goal finished/result.
	 */
	public ITerminableFuture<?>	getFinished()
	{
		if(finished==null)
		{
			finished = new TerminableFuture<>(reason -> 
			{
				exception = reason;
				System.out.println("drop: "+this+", "+reason);
				drop();	
			});
		}
		return finished;
	}

    public IFuture<GoalState> evaluateGoalState()
    {
        IComponent component = IComponentManager.get().getCurrentComponent();
        Map<String, Object> beliefs = BeliefExtractor.extract(component);
        return component.getFeature(IBDINGAgentFeature.class).getReasoner().evaluateGoalState(this, beliefs);
    }

	/**
	 *  Test if the element is finished.
	 */
	public IFuture<Boolean> isFinished()
	{
        Future<Boolean> ret = new Future<>();

        evaluateGoalState().then(state ->
        {
            if(state==GoalState.SUCCEEDED || state==GoalState.FAILED || state==GoalState.DROPPED)
            {
               ret.setResult(Boolean.TRUE);
            }
            else
            {
                ret.setResult(Boolean.FALSE);
            }
        });

        return ret;
	}

    protected boolean isFinished(GoalState state)
    {
        return state==GoalState.SUCCEEDED || state==GoalState.FAILED || state==GoalState.DROPPED;
    }

    public IntentionHistory getHistory() 
    {
        return history;
    }

    public RGoal setHistory(IntentionHistory history) 
    {
        this.history = history;
        return this;
    }

    public Map<String, Object> getParameters() 
    {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) 
    {
        this.parameters = parameters;
    }

    public RPlan getParentPlan()
    {
        return parentPlan;
    }

    public void setParentPlan(RPlan parentPlan)
    {
        this.parentPlan = parentPlan;
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
        RGoal other = (RGoal) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() 
    {
        return "RGoal [type="+goal.getName()+", parameters=" + parameters + "]";
    }

}
