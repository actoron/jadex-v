package jadex.bdi.university;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IGoal;
import jadex.bdi.IPlan;
import jadex.bdi.PlanFailureException;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanPrecondition;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

/**
 *  Go to university example taken from  
 *  Winikoff, Padgham: developing intelligent agent systems, 2004.
 */
@BDIAgent
public class UniversityAgent
{
	/** The bdi agent. */
	@Inject
	protected IComponent agent;
	
	/** Belief if it is currently raining. */
//	@Belief
	protected boolean raining;
	
	/** Belief if wait time is not too long. */
//	@Belief
	protected boolean waiting;
	
	/** The top-level goal to come to the university. */
	@Goal
	protected class ComeToUniGoal
	{
	}

	/**
	 *  Create the agent pojo with given initial beliefs.
	 */
	public UniversityAgent(boolean raining, boolean waiting)
	{
		this.raining	= raining;
		this.waiting	= waiting;
	}
	
	/** The take x goal is for using a train or tram. */
	@Goal
	protected static class TakeXGoal
	{
		public enum Type{TRAIN, TRAM};
		
		protected Type type;
		
		public TakeXGoal(Type type)
		{
			this.type = type;
		}

		public Type getType()
		{
			return type;
		}
	}
	
	/** 
	 *  The agent body is executed on startup.
	 *  It creates and dispatches a come to university goal.
	 */
	//@AgentBody
	@OnStart
	public void body()
	{
		System.out.println("raining: "+raining);
		System.out.println("waiting: "+waiting);
		try
		{
			agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new ComeToUniGoal()).get();
		}
		catch(Exception e)
		{
			System.out.println("stayed at home");
		}
		agent.terminate();
	}
	
	/**
	 *  The walk plan for the come to university goal.
	 *  Walk only if its not raining and not as first choice
	 */
	@Plan(trigger=@Trigger(goals=ComeToUniGoal.class)/*, priority=-1*/)
	protected class WalkPlan
	{
		@PlanPrecondition
		protected boolean checkWeather()
		{
			return !raining;
		}
		
		@PlanBody
		protected void walk()
		{
			System.out.println("Walked to Uni.");
		}
	}
	
	/**
	 *  The train plan for the come to university goal.
	 *  Only take train when its raining (too expensive)
	 */
	@Plan(trigger=@Trigger(goals=ComeToUniGoal.class))
	protected class TrainPlan
	{
		@PlanPrecondition
		protected boolean checkWeather()
		{
			return raining;
		}
		
		@PlanBody
		protected void takeTrain(IPlan plan)
		{
			System.out.println("Trying to take train to Uni.");
			plan.dispatchSubgoal(new TakeXGoal(TakeXGoal.Type.TRAIN)).get();
			System.out.println("Took train to Uni.");
		}
	}

	/**
	 *  The tram plan for come to university goal.
	 *  Tram is always a good idea.
	 */
	@Plan(trigger=@Trigger(goals=ComeToUniGoal.class))
	protected void tramPlan(IPlan plan)
	{
		System.out.println("Trying to take tram to Uni.");
		plan.dispatchSubgoal(new TakeXGoal(TakeXGoal.Type.TRAM)).get();
		System.out.println("Took tram to Uni.");
	}
	
	/**
	 *  Take X plan for the take X goal.
	 */
	@Plan(trigger=@Trigger(goals=TakeXGoal.class))
	protected void takeX(/*TakeXGoal goal*/IPlan plan)
	{
		System.out.println("Walking to station.");
		System.out.println("Checking time table.");
		if(!waiting)
		{
			System.out.println("Wait time is too long, failed.");
			throw new PlanFailureException();
		}
		else
		{
//			System.out.println("Taking "+goal.getType());
			System.out.println("Taking "+((TakeXGoal)((IGoal)plan.getReason()).getPojo()).getType());
		}
	}
}
