package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IGoal;
import jadex.bdi.IPlan;
import jadex.bdi.IPlan.GoalFinishedEvent;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IThrowingConsumer;
import jadex.future.Future;

/**
 *  Test all kinds of plan triggers
 */
public class PlanTriggerTest
{
	@BDIAgent
	static class PlanTriggerTestAgent
	{
		@Belief
		List<String>	bel	= new ArrayList<>();
		
		Future<jadex.core.ChangeEvent>	added	= new Future<>();
		Future<jadex.core.ChangeEvent>	changed	= new Future<>();
		Future<jadex.core.ChangeEvent>	removed	= new Future<>();
		Future<IGoal>	goal	= new Future<>();
		Future<GoalFinishedEvent>	goalfinished	= new Future<>();
		
		@Goal
		class MyGoal{}
		
		@Plan(trigger=@Trigger(factadded="bel"))
		void addedPlan(IPlan plan)
		{
			added.setResult((jadex.core.ChangeEvent) plan.getReason());
		}

		@Plan(trigger=@Trigger(factchanged="bel"))
		void changedPlan(IPlan plan)
		{
			changed.setResult((jadex.core.ChangeEvent) plan.getReason());
		}

		@Plan(trigger=@Trigger(factremoved="bel"))
		void removedPlan(IPlan plan)
		{
			removed.setResult((jadex.core.ChangeEvent) plan.getReason());
		}

		@Plan(trigger=@Trigger(goals=MyGoal.class))
		void goalPlan(IPlan plan)
		{
			goal.setResult((IGoal) plan.getReason());
		}
		
		@Plan(trigger=@Trigger(goalfinisheds=MyGoal.class))
		void goalFinishedPlan(IPlan plan)
		{
			goalfinished.setResult((GoalFinishedEvent) plan.getReason());
		}
	}
	
	@Test
	void testFactAdded()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		agent.scheduleStep(() -> pojo.bel.add(0, "new fact"));
		checkEventInfo(pojo.added, "bel", Type.ADDED, null, "new fact", 0);
	}
	
	@Test
	void testFactChanged()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		agent.scheduleStep(() -> 
		{
			pojo.bel.add("old fact");
			pojo.bel.set(0, "new fact");
		});		
		checkEventInfo(pojo.changed, "bel", Type.CHANGED, "old fact", "new fact", 0);
	}

	@Test
	void testFactRemoved()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		agent.scheduleStep(() -> 
		{
			pojo.bel.add("new fact");
			pojo.bel.remove(0);			
		});
		checkEventInfo(pojo.removed, "bel", Type.REMOVED, null, "new fact", 0);
	}
	
	@Test
	void testGoal()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		agent.scheduleStep((IThrowingConsumer<IComponent>)ia -> ia.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal()));
		checkGoalInfo(pojo.goal, PlanTriggerTestAgent.MyGoal.class);
	}

	@Test
	void testGoalFinished()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		agent.scheduleStep((IThrowingConsumer<IComponent>)ia -> ia.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal()));
		checkGoalEventInfo(pojo.goalfinished, PlanTriggerTestAgent.MyGoal.class);
	}

	/**
	 *  Check if old/new value and info match expectations.
	 */
	public static void checkEventInfo(Future<jadex.core.ChangeEvent> fut, String name, Type type, Object oldval, Object newval, Object info)
	{
		jadex.core.ChangeEvent	event	= fut.get(TestHelper.TIMEOUT);
		assertEquals(name, event.name(), "name");
		assertEquals(type, event.type(), "type");
		
		assertEquals(oldval, event.oldvalue(), "old value");
		assertEquals(newval, event.value(), "new value");
		assertEquals(info, event.info(), "info");
	}

	/**
	 *  Check if goal matches expectations.
	 */
	public static void checkGoalInfo(Future<IGoal> fut, Class<?> pojoclass)
	{
		IGoal	goal	= fut.get(TestHelper.TIMEOUT);
		assertEquals(pojoclass, goal.getPojo().getClass(), "pojo");
	}

	/**
	 *  Check if old/new value and info match expectations.
	 */
	public static void checkGoalEventInfo(Future<GoalFinishedEvent> fut, Class<?> pojoclass)
	{
		GoalFinishedEvent	event	= fut.get(TestHelper.TIMEOUT);
		assertEquals(pojoclass, event.goal().getPojo().getClass(), "pojo");
	}
}

