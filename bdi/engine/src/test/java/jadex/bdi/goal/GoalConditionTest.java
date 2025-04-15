package jadex.bdi.goal;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;

/**
 *  Test various goal conditions
 */
public class GoalConditionTest
{
	@Test
	public void	testConstructorCreationCondition()
	{
		@BDIAgent
		class GoalCreationAgent
		{
			Future<String>	created	= new Future<>();
			Future<String>	processed	= new Future<>();
			
			@Belief
			List<String> trigger	= new ArrayList<>();
			
			@Goal
			class StartGoal
			{
				String	param;
				
				@GoalCreationCondition(factadded="trigger")
				public StartGoal(String param)
				{
					this.param	= param;
					created.setResult(param);
				}
			}
			
			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process(StartGoal goal)
			{
				processed.setResult(goal.param);
			}
		}
		
		GoalCreationAgent	pojo	= new GoalCreationAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> pojo.trigger.add("value")).get(TestHelper.TIMEOUT);
		assertEquals("value", pojo.created.get(TestHelper.TIMEOUT));
		assertEquals("value", pojo.processed.get(TestHelper.TIMEOUT));
	}

	@Test
	public void	testMethodCreationCondition()
	{
		@BDIAgent
		class GoalCreationAgent
		{
			Future<String>	created	= new Future<>();
			Future<String>	processed	= new Future<>();
			
			@Belief
			List<String> trigger	= new ArrayList<>();
			
			@Goal
			class StartGoal
			{
				String	param;
				
				@GoalCreationCondition(factadded="trigger")
				public static StartGoal create(String param, GoalCreationAgent pojo)
				{
					return pojo.new StartGoal(param);
				}
				
				public StartGoal(String param)
				{
					this.param	= param;
					created.setResult(param);
				}
			}
			
			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process(StartGoal goal)
			{
				processed.setResult(goal.param);
			}
		}
		
		GoalCreationAgent	pojo	= new GoalCreationAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> pojo.trigger.add("value")).get(TestHelper.TIMEOUT);
		assertEquals("value", pojo.created.get(TestHelper.TIMEOUT));
		assertEquals("value", pojo.processed.get(TestHelper.TIMEOUT));
	}

	@Test
	public void	testBooleanMethodCreationCondition()
	{
		@BDIAgent
		class GoalCreationAgent
		{
			Future<String>	created	= new Future<>();
			Future<String>	processed	= new Future<>();
			
			@Belief
			List<String> trigger	= new ArrayList<>();
			
			@Goal
			class StartGoal
			{
				String	param;
				
				@GoalCreationCondition(factadded="trigger")
				static boolean condition(String param)
				{
					return "value".equals(param);
				}
				
				@SuppressWarnings("unused")
				public StartGoal(String param)
				{
					this.param	= param;
					created.setResult(param);
				}
			}
			
			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process(StartGoal goal)
			{
				processed.setResult(goal.param);
			}
		}
		
		GoalCreationAgent	pojo	= new GoalCreationAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> pojo.trigger.add("wrong")).get(TestHelper.TIMEOUT);
		assertFalse(pojo.created.isDone());
		handle.scheduleStep(() -> pojo.trigger.add("value")).get(TestHelper.TIMEOUT);
		assertEquals("value", pojo.created.get(TestHelper.TIMEOUT));
		assertEquals("value", pojo.processed.get(TestHelper.TIMEOUT));
	}
}
