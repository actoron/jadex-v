package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.ChangeEvent;
import jadex.bdi.runtime.IBDIAgent;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IPlan;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.IThrowingConsumer;
import jadex.future.Future;
import jadex.micro.annotation.Agent;
import jadex.rules.eca.ChangeInfo;

/**
 *  Test all kinds of plan triggers
 */
public class PlanTriggerTest
{
	@Agent(type="bdip")
	static class PlanTriggerTestAgent
	{
		@Belief
		List<String>	bel	= new ArrayList<>();
		
		Future<Object>	added	= new Future<>();
		Future<Object>	changed1	= new Future<>();
		Future<Object>	changed2	= new Future<>();
		Future<Object>	removed	= new Future<>();
		Future<Object>	goal	= new Future<>();
		Future<Object>	goalfinished	= new Future<>();
		
		@Goal
		class MyGoal{}
		
		@Plan(trigger=@Trigger(factadded="bel"))
		void addedPlan(IPlan plan)
		{
			added.setResult(plan.getReason());
		}

		@Plan(trigger=@Trigger(factchanged="bel"))
		void changedPlan(IPlan plan)
		{
			if(!changed1.isDone())
			{
				changed1.setResult(plan.getReason());
			}
			else
			{
				changed2.setResult(plan.getReason());
			}
		}

		@Plan(trigger=@Trigger(factremoved="bel"))
		void removedPlan(IPlan plan)
		{
			removed.setResult(plan.getReason());
		}

		@Plan(trigger=@Trigger(goals=MyGoal.class))
		void goalPlan(IPlan plan)
		{
			goal.setResult(plan.getReason());
		}
		
		@Plan(trigger=@Trigger(goalfinisheds=MyGoal.class))
		void goalFinishedPlan(IPlan plan)
		{
			goalfinished.setResult(plan.getReason());
		}
	}
	
	@Test
	void testFactAdded()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IExternalAccess	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep(() -> pojo.bel.add(0, "new fact"));
		checkEventInfo(pojo.added, "bel", "factadded", null, "new fact", 0);
	}
	
	@Test
	void testFactChanged()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IExternalAccess	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep(() -> 
		{
			pojo.bel.add("old fact");
			pojo.bel.set(0, "new fact");
		});
		checkEventInfo(pojo.changed1, "bel", "beliefchanged", null, Arrays.asList(new String[]{"new fact"}), null);
		checkEventInfo(pojo.changed2, "bel", "factchanged", "old fact", "new fact", 0);
	}

	@Test
	void testFactRemoved()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IExternalAccess	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep(() -> 
		{
			pojo.bel.add("new fact");
			pojo.bel.remove(0);			
		});
		checkEventInfo(pojo.removed, "bel", "factremoved", null, "new fact", 0);
	}
	
	@Test
	void testGoal()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IExternalAccess	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep((IThrowingConsumer<IComponent>)ia -> ia.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal()));
		checkGoalInfo(pojo.goal, PlanTriggerTestAgent.MyGoal.class);
	}

	@Test
	void testGoalFinished()
	{
		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
		IExternalAccess	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep((IThrowingConsumer<IComponent>)ia -> ia.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal()));
		checkGoalEventInfo(pojo.goalfinished, PlanTriggerTestAgent.MyGoal.class.getName(), "goaldropped", PlanTriggerTestAgent.MyGoal.class);
	}

	/**
	 *  Check if old/new value and info match expectations.
	 */
	public static void checkEventInfo(Future<Object> fut, String source, String type, Object oldval, Object newval, Object info)
	{
		Object	event	= fut.get(1000);
		assertInstanceOf(ChangeEvent.class, event, "reason");
		assertEquals(source, ((ChangeEvent<?>)event).getSource(), "source");
		assertEquals(type, ((ChangeEvent<?>)event).getType(), "type");
		
		@SuppressWarnings("unchecked")
		ChangeInfo<Object>	ci	= (ChangeInfo<Object>)((ChangeEvent<?>)event).getValue();
		assertEquals(oldval, ci.getOldValue(), "old value");
		assertEquals(newval, ci.getValue(), "new value");
		assertEquals(info, ci.getInfo(), "info");
	}

	/**
	 *  Check if goal matches expectations.
	 */
	public static void checkGoalInfo(Future<Object> fut, Class<?> pojoclass)
	{
		Object	goal	= fut.get(1000);
		assertInstanceOf(IGoal.class, goal, "reason");
		assertEquals(pojoclass, ((IGoal)goal).getPojo().getClass(), "pojo");
	}

	/**
	 *  Check if old/new value and info match expectations.
	 */
	public static void checkGoalEventInfo(Future<Object> fut, String source, String type, Class<?> pojoclass)
	{
		Object	event	= fut.get(1000);
		assertInstanceOf(ChangeEvent.class, event, "reason");
		assertEquals(source, ((ChangeEvent<?>)event).getSource(), "source");
		assertEquals(type, ((ChangeEvent<?>)event).getType(), "type");
		assertInstanceOf(IGoal.class, ((ChangeEvent<?>)event).getValue());
		assertEquals(pojoclass, ((IGoal)((ChangeEvent<?>)event).getValue()).getPojo().getClass(), "pojo");
	}
}

