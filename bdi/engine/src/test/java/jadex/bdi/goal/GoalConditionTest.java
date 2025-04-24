package jadex.bdi.goal;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.ExcludeMode;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalMaintainCondition;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;

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
					return "value".equals(param) ? pojo.new StartGoal(param) : null;
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
		handle.scheduleStep(() -> pojo.trigger.add("wrong")).get(TestHelper.TIMEOUT);
		assertFalse(pojo.created.isDone());
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

	@Test
	public void	testTargetCondition()
	{
		@BDIAgent
		class GoalTargetAgent
		{
			@Belief
			List<String> trigger	= new ArrayList<>(Collections.singletonList("value"));
			
			@Goal
			class StartGoal
			{
				@GoalTargetCondition(beliefs="trigger")
				boolean targetCondition(String fact)
				{
					return "value".equals(fact) && trigger.contains("value");
				}
			}
			
			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process(StartGoal goal)
			{
				// Should not trigger on remove
				trigger.remove("value");

				// Should not trigger on wrong add/remove
				trigger.add("wrong");
				trigger.remove("wrong");

				// Should trigger on value add
				trigger.add("value");
				
				// Plan should be aborted, so this shouldn't execute.
				trigger.add("wrong");
			}
		}
		
		GoalTargetAgent	pojo	= new GoalTargetAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new StartGoal())).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		assertEquals(Collections.singletonList("value"), pojo.trigger);
	}

	@Test
	public void	testMaintainCondition()
	{
		@BDIAgent
		class GoalMaintainAgent
		{
			@Belief
			List<String> trigger;
			
			@Goal
			class StartGoal
			{
				@GoalMaintainCondition(beliefs="trigger")
				boolean maintainCondition()
				{
					return trigger.contains("value");
				}
			}

			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process(StartGoal goal)
			{
				trigger.add("value");
			}
		}
		
		GoalMaintainAgent	pojo	= new GoalMaintainAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new StartGoal())).get(TestHelper.TIMEOUT);
		assertEquals(Collections.singletonList("value"), pojo.trigger);
		handle.scheduleStep(() -> pojo.trigger.removeFirst()).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		assertEquals(Collections.singletonList("value"), pojo.trigger);
	}

	@Test
	public void	testMaintainTargetCondition()
	{
		@BDIAgent
		class GoalMaintainAgent
		{
			@Belief
			List<String> trigger	= new ArrayList<>(Collections.singletonList("value"));
			
			@Goal(excludemode=ExcludeMode.WhenFailed)
			class StartGoal
			{
				@GoalMaintainCondition(beliefs="trigger")
				boolean maintainCondition()
				{
					return trigger.contains("value");
				}
				
				@GoalTargetCondition(beliefs="trigger")
				boolean targetCondition()
				{
					return trigger.size()>1;
				}
			}

			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process(StartGoal goal)
			{
				trigger.add("value");
			}
		}
		
		GoalMaintainAgent	pojo	= new GoalMaintainAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		IFuture<Void>	goalfut	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new StartGoal()));
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		assertFalse(goalfut.isDone()); // Should stay idle initially
		assertEquals(Collections.singletonList("value"), pojo.trigger);
		
		handle.scheduleStep(() -> pojo.trigger.removeFirst()).get(TestHelper.TIMEOUT);
		assertNull(goalfut.get(TestHelper.TIMEOUT));
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		assertEquals(Arrays.asList("value", "value"), pojo.trigger);
	}
}
