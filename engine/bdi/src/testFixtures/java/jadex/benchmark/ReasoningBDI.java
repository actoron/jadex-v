package jadex.benchmark;


import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.IBDIAgentFeature;
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
