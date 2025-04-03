package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;

import jadex.bdi.IPlan;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.impl.ChangeEvent;
import jadex.future.Future;
import jadex.rules.eca.ChangeInfo;

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
		
		Future<Object>	added	= new Future<>();
		Future<Object>	changed	= new Future<>();
		Future<Object>	removed	= new Future<>();
		Future<Object>	goal	= new Future<>();
		Future<Object>	goalfinished	= new Future<>();
		
//		@Goal
//		class MyGoal{}
		
		@Plan(trigger=@Trigger(factadded="bel"))
		void addedPlan(IPlan plan)
		{
			added.setResult(plan.getReason());
		}

		@Plan(trigger=@Trigger(factchanged="bel"))
		void changedPlan(IPlan plan)
		{
			ChangeEvent<?>	event	= (ChangeEvent<?>)plan.getReason();
			// Ignore initial "beliefchanged" event
			if("factchanged".equals(event.getType()))
			{
				changed.setResult(event);
			}
		}

		@Plan(trigger=@Trigger(factremoved="bel"))
		void removedPlan(IPlan plan)
		{
			removed.setResult(plan.getReason());
		}

//		@Plan(trigger=@Trigger(goals=MyGoal.class))
//		void goalPlan(IPlan plan)
//		{
//			goal.setResult(plan.getReason());
//		}
//		
//		@Plan(trigger=@Trigger(goalfinisheds=MyGoal.class))
//		void goalFinishedPlan(IPlan plan)
//		{
//			goalfinished.setResult(plan.getReason());
//		}
	}
	
//	@Test
//	void testFactAdded()
//	{
//		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
//		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
//		agent.scheduleStep(() -> pojo.bel.add(0, "new fact"));
//		checkEventInfo(pojo.added, "bel", "factadded", null, "new fact", 0);
//	}
//	
//	@Test
//	void testFactChanged()
//	{
//		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
//		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
//		agent.scheduleStep(() -> 
//		{
//			pojo.bel.add("old fact");
//			pojo.bel.set(0, "new fact");
//		});		
//		checkEventInfo(pojo.changed, "bel", "factchanged", "old fact", "new fact", 0);
//	}
//
//	@Test
//	void testFactRemoved()
//	{
//		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
//		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
//		agent.scheduleStep(() -> 
//		{
//			pojo.bel.add("new fact");
//			pojo.bel.remove(0);			
//		});
//		checkEventInfo(pojo.removed, "bel", "factremoved", null, "new fact", 0);
//	}
	
//	@Test
//	void testGoal()
//	{
//		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
//		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
//		agent.scheduleStep((IThrowingConsumer<IComponent>)ia -> ia.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal()));
//		checkGoalInfo(pojo.goal, PlanTriggerTestAgent.MyGoal.class);
//	}

//	@Test
//	void testGoalFinished()
//	{
//		PlanTriggerTestAgent	pojo	= new PlanTriggerTestAgent();
//		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
//		agent.scheduleStep((IThrowingConsumer<IComponent>)ia -> ia.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal()));
//		checkGoalEventInfo(pojo.goalfinished, PlanTriggerTestAgent.MyGoal.class.getName(), "goaldropped", PlanTriggerTestAgent.MyGoal.class);
//	}

	/**
	 *  Check if old/new value and info match expectations.
	 */
	public static void checkEventInfo(Future<Object> fut, String source, String type, Object oldval, Object newval, Object info)
	{
		Object	event	= fut.get(TestHelper.TIMEOUT);
		assertInstanceOf(ChangeEvent.class, event, "reason");
		assertEquals(source, ((ChangeEvent<?>)event).getSource(), "source");
		assertEquals(type, ((ChangeEvent<?>)event).getType(), "type");
		
		@SuppressWarnings("unchecked")
		ChangeInfo<Object>	ci	= (ChangeInfo<Object>)((ChangeEvent<?>)event).getValue();
		assertEquals(oldval, ci.getOldValue(), "old value");
		assertEquals(newval, ci.getValue(), "new value");
		assertEquals(info, ci.getInfo(), "info");
	}

//	/**
//	 *  Check if goal matches expectations.
//	 */
//	public static void checkGoalInfo(Future<Object> fut, Class<?> pojoclass)
//	{
//		Object	goal	= fut.get(TestHelper.TIMEOUT);
//		assertInstanceOf(IGoal.class, goal, "reason");
//		assertEquals(pojoclass, ((IGoal)goal).getPojo().getClass(), "pojo");
//	}

//	/**
//	 *  Check if old/new value and info match expectations.
//	 */
//	public static void checkGoalEventInfo(Future<Object> fut, String source, String type, Class<?> pojoclass)
//	{
//		Object	event	= fut.get(TestHelper.TIMEOUT);
//		assertInstanceOf(ChangeEvent.class, event, "reason");
//		assertEquals(source, ((ChangeEvent<?>)event).getSource(), "source");
//		assertEquals(type, ((ChangeEvent<?>)event).getType(), "type");
//		assertInstanceOf(IGoal.class, ((ChangeEvent<?>)event).getValue());
//		assertEquals(pojoclass, ((IGoal)((ChangeEvent<?>)event).getValue()).getPojo().getClass(), "pojo");
//	}
}

