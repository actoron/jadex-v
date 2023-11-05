package jadex.benchmark;


import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalCreationCondition;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.rules.eca.annotations.Event;

/**
 *  Change a belief -> create a goal -> execute a plan
 */
@Agent
public class ReasoningBDI
{
	@Belief
	private boolean start;
	
	@Goal
	public class StartGoal
	{
		@GoalCreationCondition()
//		@GoalCreationCondition(factchanged = "start")	// TODO: why does not work?
		public StartGoal(@Event("start") boolean dummy)
		{
//			System.out.println("goal: "+this);
		}
	}
	
	@OnStart
	public void body()
	{
//		System.out.println("body: "+this);
		start	= true;
	}
	
	/**
	 *  First plan. Fails with exception.
	 */
	@Plan(trigger=@Trigger(goals=StartGoal.class))
	protected void	printHello1(StartGoal goal, IComponent agent)
	{
//		System.out.println("plan: "+this);
		IBDIAgentFeature	bdif	= agent.getFeature(IBDIAgentFeature.class);
		@SuppressWarnings("unchecked")
		Future<ComponentIdentifier>	cid	= (Future<ComponentIdentifier>)bdif.getArgument("cid");
		cid.setResult(agent.getId());
	}
}
