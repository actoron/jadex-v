package jadex.bdi.goal;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jadex.bdi.GoalDroppedException;
import jadex.bdi.GoalFailureException;
import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.ExcludeMode;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalContextCondition;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalDropCondition;
import jadex.bdi.annotation.GoalMaintainCondition;
import jadex.bdi.annotation.GoalQueryCondition;
import jadex.bdi.annotation.GoalRecurCondition;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
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
	public void	testNoPlanProceduralQueryGoal()
	{
		@BDIAgent
		class GoalQueryAgent
		{
			List<String>	executions	= new ArrayList<>();
			
			@Goal
			class StartGoal	implements Supplier<String>
			{
				public String get()
				{
					return "value";
				}
			}

			// Goal should be immediately succeeded, so this shouldn't execute.
			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process2(StartGoal goal)
			{
				executions.add("wrong");
			}
		}
		
		GoalQueryAgent	pojo	= new GoalQueryAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		String value	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new StartGoal())).get(TestHelper.TIMEOUT);
		assertEquals("value", value);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		assertEquals(0, pojo.executions.size());
	}

	@Test
	public void	testProceduralQueryGoal()
	{
		@BDIAgent
		class GoalQueryAgent
		{
			@Belief
			List<String> trigger	= new ArrayList<>();
			
			@Goal
			class StartGoal	implements Supplier<String>
			{
				public String get()
				{
					return trigger.isEmpty() ? null : trigger.get(0);
				}
			}

			// Should not succeed on nop
			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	nop(StartGoal goal) {}

			// Should succeed on value add
			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process(StartGoal goal)
			{
				trigger.add("value");
			}
			
			// Goal should be succeeded, so this shouldn't execute.
			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process2(StartGoal goal)
			{
				trigger.add("wrong");
			}
		}
		
		GoalQueryAgent	pojo	= new GoalQueryAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		String value	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new StartGoal())).get(TestHelper.TIMEOUT);
		assertEquals("value", value);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		assertEquals(Collections.singletonList("value"), pojo.trigger);
	}

	@Test
	public void	testQueryCondition()
	{
		@BDIAgent
		class GoalQueryAgent
		{
			@Belief
			List<String> trigger	= new ArrayList<>(Collections.singletonList("value"));
			
			@Goal
			class StartGoal	implements Supplier<String>
			{
				@GoalQueryCondition
				public String get()
				{
					return trigger.isEmpty() ? null : trigger.get(0);
				}
			}
			
			@Plan(trigger=@Trigger(goals=StartGoal.class))
			protected void	process(StartGoal goal)
			{
				// Should not trigger on remove
				trigger.remove("value");

				// Should trigger on value add
				trigger.add("value");
				
				// Plan should be aborted, so this shouldn't execute.
				trigger.add("wrong");
			}
		}
		
		GoalQueryAgent	pojo	= new GoalQueryAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		String value	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new StartGoal())).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		assertEquals("value", value);
		assertEquals(Collections.singletonList("value"), pojo.trigger);
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
				@GoalTargetCondition
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
	public void	testTargetWithResult()
	{
		@BDIAgent
		class GoalTargetResultAgent
		{
			@Belief
			List<String> trigger	= new ArrayList<>(Collections.singletonList("value"));
			
			@Goal
			class StartGoal	implements Supplier<String>
			{
				@GoalTargetCondition
				boolean targetCondition(String fact)
				{
					return "value".equals(fact) && trigger.contains("value");
				}
				
				@Override
				public String get()
				{
					return trigger.get(0);
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
		
		GoalTargetResultAgent	pojo	= new GoalTargetResultAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		String	result	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new StartGoal())).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		assertEquals("value", result);
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
				@GoalMaintainCondition
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
				@GoalMaintainCondition
				boolean maintainCondition()
				{
					return trigger.contains("value");
				}
				
				@GoalTargetCondition
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

	@Test
	public void	testContextCondition()
	{
		@BDIAgent
		class GoalContextAgent
		{
			@Belief
			Val<Boolean>	context	= new Val<>(false);
			
			@Goal
			class ContextGoal
			{
				@GoalContextCondition
				boolean	context(Boolean value)
				{
					// value is null on initial check (not triggered by belief change)
					return value==null ? context.get() : value;
				}
			}
		}
		
		GoalContextAgent	pojo	= new GoalContextAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		IFuture<Void>	goalfut	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new ContextGoal()));
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertFalse(goalfut.isDone()); // Should be suspended initially
		
		handle.scheduleStep(() -> {pojo.context.set(false); return null;}).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertFalse(goalfut.isDone()); // Should still be suspended
		
		handle.scheduleStep(() -> {pojo.context.set(true); return null;}).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertTrue(goalfut.isDone()); // Should be processed.
		assertThrows(GoalFailureException.class, () -> goalfut.get(TestHelper.TIMEOUT));
	}

	@Test
	public void	testDropCondition()
	{
		@BDIAgent
		class GoalDropAgent
		{
			@Belief
			Val<Boolean>	drop	= new Val<>(false);
			
			Future<Void>	aborted	= new Future<>();
			
			@Goal
			class DropGoal
			{
				@GoalDropCondition
				boolean	drop(Boolean value)
				{
					// value is null on initial check (not triggered by belief change)
					return value==null ? drop.get() : value;
				}
			}
			
			@Plan(trigger=@Trigger(goals=DropGoal.class))
			class blockPlan
			{
				@PlanBody
				void body()
				{
					new Future<>().get();
				}
				
				@PlanAborted
				void abort()
				{
					aborted.setResult(null);
				}
			}
		}
		
		GoalDropAgent	pojo	= new GoalDropAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		IFuture<Void>	goalfut	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new DropGoal()));
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertFalse(goalfut.isDone()); // Should be processing
		assertFalse(pojo.aborted.isDone());
		
		handle.scheduleStep(() -> {pojo.drop.set(false); return null;}).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertFalse(goalfut.isDone()); // Should still be processing
		assertFalse(pojo.aborted.isDone());
		
		handle.scheduleStep(() -> {pojo.drop.set(true); return null;}).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertTrue(goalfut.isDone()); // Should be failed.
		assertThrows(GoalDroppedException.class, () -> goalfut.get(TestHelper.TIMEOUT));
		assertNull(pojo.aborted.get(TestHelper.TIMEOUT));
	}

	@Test
	public void	testRecurCondition()
	{
		@BDIAgent
		class GoalRecurAgent
		{
			@Belief
			Val<Boolean>	recur	= new Val<>(false);
			
			int plancnt	= 0;
			
			@Goal
			class RecurGoal
			{
				@GoalRecurCondition
				boolean	recur(Boolean value)
				{
					// value is null on initial check (not triggered by belief change)
					return value==null ? recur.get() : value;
				}
			}
			
			@Plan(trigger=@Trigger(goals=RecurGoal.class))
			class myPlan
			{
				@PlanBody
				void body()
				{
					plancnt++;
				}
			}
		}
		
		GoalRecurAgent	pojo	= new GoalRecurAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		IFuture<Void>	goalfut	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new RecurGoal()));
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertFalse(goalfut.isDone()); // Should be paused
		assertEquals(1, pojo.plancnt);
		
		handle.scheduleStep(() -> {pojo.recur.set(false); return null;}).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertFalse(goalfut.isDone()); // Should still be paused
		assertEquals(1, pojo.plancnt);
		
		handle.scheduleStep(() -> {pojo.recur.set(true); return null;}).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertFalse(goalfut.isDone()); // Should still be paused
		assertEquals(2, pojo.plancnt);
	}
}
