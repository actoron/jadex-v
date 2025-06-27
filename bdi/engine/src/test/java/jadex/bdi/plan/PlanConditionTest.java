package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.IPlan;
import jadex.bdi.TestHelper;
import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanContextCondition;
import jadex.bdi.annotation.PlanPrecondition;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;

/**
 *  Test plan pre- and context conditions
 */
public class PlanConditionTest
{
	@BDIAgent
	static class PlanConditionTestAgent
	{
		@Belief
		Val<Boolean>	bel	= new Val<>(true);

		@Belief
		List<String>	trigger	= new ArrayList<>();

		Future<String>	prefut	= new Future<>();
		Future<String>	contextfut	= new Future<>();
		
		@Plan(trigger=@Trigger(factadded="trigger"))
		class PrePlan1
		{
			@PlanPrecondition
			boolean precond()
			{
				return bel.get();
			}
			
			@PlanBody
			void body(IPlan plan)
			{
				prefut.setResultIfUndone(plan.getModelName());
			}
		}
		
		@Plan(trigger=@Trigger(factadded="trigger"))
		class PrePlan2
		{
			@PlanPrecondition
			boolean precond()
			{
				return !bel.get();
			}
			
			@PlanBody
			void body(IPlan plan)
			{
				prefut.setResultIfUndone(plan.getModelName());
			}
		}
		
		@Plan(trigger=@Trigger(factadded="trigger"))
		class PrePlan3
		{
			@PlanPrecondition
			static boolean precond()
			{
				return false;
			}
			
			public PrePlan3()
			{
				// Should not be called due to static precondition
				throw new RuntimeException("Cannot be instantiated.");
			}
		}
		
		@Plan(trigger=@Trigger(factadded="trigger"))
		class ContextPlan
		{
			@PlanContextCondition
			boolean context()
			{
				return bel.get();
			}
			
			@PlanBody
			void body(IPlan plan, String fact)
			{
				if("go".equals(fact))
				{
					// Trigger self-abort
					bel.set(false);
				}
				else
				{
					// Wait and be aborted from outside
					new Future<Void>().get(TestHelper.TIMEOUT);
				}
				contextfut.setResultIfUndone("not aborted");
			}
			
			@PlanAborted
			void aborted()
			{
				contextfut.setResultIfUndone("aborted");				
			}
		}
	}
	
	@Test
	void testPlanPrecondition()
	{
		// First plan
		{
			PlanConditionTestAgent	pojo	= new PlanConditionTestAgent();
			IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
			agent.scheduleStep(() -> pojo.trigger.add("go")).get(TestHelper.TIMEOUT);
			assertEquals(PlanConditionTestAgent.PrePlan1.class.getName(), pojo.prefut.get(TestHelper.TIMEOUT));
		}
		
		// Second plan
		{
			PlanConditionTestAgent	pojo	= new PlanConditionTestAgent();
			IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
			agent.scheduleStep(() ->
			{
				pojo.bel.set(false);
				pojo.trigger.add("go");
				return null;
			}).get(TestHelper.TIMEOUT);
			assertEquals(PlanConditionTestAgent.PrePlan2.class.getName(), pojo.prefut.get(TestHelper.TIMEOUT));
		}
	}
	
	@Test
	void testPlanSelfabort()
	{
		PlanConditionTestAgent	pojo	= new PlanConditionTestAgent();
		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		agent.scheduleStep(() -> pojo.trigger.add("go")).get(TestHelper.TIMEOUT);
		assertEquals("aborted", pojo.contextfut.get(TestHelper.TIMEOUT));
	}
	
	@Test
	void testPlanExtabort()
	{
		PlanConditionTestAgent	pojo	= new PlanConditionTestAgent();
		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		agent.scheduleStep(() -> pojo.trigger.add("wait")).get(TestHelper.TIMEOUT);
		agent.scheduleStep(() -> {pojo.bel.set(false); return null;}).get(TestHelper.TIMEOUT);
		assertEquals("aborted", pojo.contextfut.get(TestHelper.TIMEOUT));
	}
}

