package jadex.bding.impl;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IReasoner;
import jadex.bding.Intention;
import jadex.bding.Goal;
import jadex.core.IComponent;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.future.IFuture;
import java.util.List;
import java.util.Set;

public class RGoal 
{
    enum GoalState
	{
		INACTIVE,
		ACTIVE,
		SUCCEEDED,
		FAILED,
		DROPPED
	}

    protected Goal goal;

    protected RIntention intention;

    protected GoalState state = GoalState.INACTIVE;

    protected IntentionHistory history;

    protected IComponent component;

    /** The finished future (if someone waits for the goal). */
	protected TerminableFuture<Object>	finished;

    protected Exception exception;

    public RGoal(Goal goal, RIntention intention, IComponent component)
    {
        this.goal = goal;
        this.intention = intention;
        this.component = component;
    }

    public void adopt()
    {
        changeState(GoalState.ACTIVE);
    }

    public void execute()
    {
        IReasoner reasoner = getComponent().getFeature(IBDINGAgentFeature.class).getReasoner();

        Set<Intention> intentions = reasoner.generateIntentions(this).get();
    
        Intention intention = reasoner.selectIntention(this, intentions).get();

        RIntention rintention = new RIntention(intention, getComponent());
        
        rintention.execute();
    }

    public void drop()
    {
        changeState(GoalState.DROPPED);
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

    protected IComponent getComponent()
    {
        return component;
    }

}
