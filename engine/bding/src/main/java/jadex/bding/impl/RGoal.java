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

public class RGoal 
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

    public RGoal(Goal goal, Map<String, Object> parameters)
    {
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
        changeState(GoalState.ACTIVE);
    }

    public IFuture<Void> execute()
    {
        Future<Void> ret = new Future<>();

        IComponent component = IComponentManager.get().getCurrentComponent();

        IReasoner reasoner = component.getFeature(IBDINGAgentFeature.class).getReasoner();

        do
        {
            BeliefSnapshot beliefs = BeliefSnapshot.extract(component);

            Set<Intention> intentions = getGoal().getIntentions();
            if(intentions.isEmpty())
            {
                intentions = reasoner.generateIntentions(this, beliefs).get();
                getGoal().setIntentions(intentions);
            }
            Intention intention = reasoner.selectIntention(this, intentions, beliefs).get();

            if(intention==null)
            {
                // todo: one could try to generate new/more intentions
                ret.setException(new RuntimeException("No intention for goal: "+this));
            }
            else
            {
                if(history.isKnown(intention))
                {
                    System.out.println("Intention generation error, known: "+intention);
                    ret.setException(new RuntimeException("Intention generation error, known: "+intention));
                }

                RIntention rintention = new RIntention(intention, this);
                setIntention(rintention);

                try
                {
                    rintention.execute().get();

                    this.state = evaluateGoalState().get();
                }
                catch(Exception e)
                {
                    history.addEntry(new IntentionHistoryEntry(rintention));
                }

                /*rintention.execute().then(Void ->
                {
                    System.out.println("Intention executed");
                }
                ).catchEx(ex ->
                {
                    // intention failed. add to history and generate new intention
                    history.addEntry(new IntentionHistoryEntry(rintention));
                });*/
            }
        }
        while(!isFinished(getState()));
        
        return ret;
    }

    public void drop()
    {
        changeState(GoalState.DROPPED);
    }

    public GoalState getState() 
    {
        return state;
    }

    public void changeState(GoalState newstate)
    {
        GoalState oldstate = state;
        state = newstate;

        if(newstate == GoalState.ACTIVE && oldstate == GoalState.INACTIVE)
        {
            execute();
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
//				System.out.println("drop: "+this+", "+reason);
				drop();	
			});
		}
		return finished;
	}

    public IFuture<GoalState> evaluateGoalState()
    {
        IComponent component = IComponentManager.get().getCurrentComponent();
        BeliefSnapshot beliefs = BeliefSnapshot.extract(component);
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

    @Override
    public String toString() 
    {
        return "RGoal [type="+goal.getName()+", parameters=" + parameters + "]";
    }

}
