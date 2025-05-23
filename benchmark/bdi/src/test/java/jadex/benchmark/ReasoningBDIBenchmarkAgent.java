package jadex.benchmark;


import java.util.ArrayList;
import java.util.List;

import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.injection.annotation.OnStart;

/**
 *  Change a belief -> create a goal -> execute a plan
 */
@BDIAgent
public class ReasoningBDIBenchmarkAgent
{
	@Belief
	private List<String> trigger	= new ArrayList<>();	
	
	Future<Void>	completed	= new Future<>();
	
	@Goal
	public class StartGoal
	{
		@GoalCreationCondition(factadded="trigger")
		public StartGoal()
		{
//			System.out.println("goal: "+this);
		}
	}
	
	@OnStart
	public void body()
	{
//		System.out.println("body: "+this);
		trigger.add("start");
	}
	
	/**
	 *  First plan. Fails with exception.
	 */
	@Plan(trigger=@Trigger(goals=StartGoal.class))
	protected void	printHello1(StartGoal goal, IComponent agent)
	{
//		System.out.println("plan: "+this);
		completed.setResult(null);
	}
}
