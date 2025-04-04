package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.TestHelper;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanContextCondition;
import jadex.bdi.annotation.PlanPrecondition;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.IBDIAgent;
import jadex.bdi.runtime.IPlan;
import jadex.bdi.runtime.Val;
import jadex.core.IComponentHandle;
import jadex.future.Future;
import jadex.micro.annotation.Agent;

/**
 *  Test plan pre- and context conditions
 */
public class PlanConditionTest
{
	@Agent(type="bdip")
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
				prefut.setResultIfUndone(plan.getModelElement().getName());
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
				prefut.setResultIfUndone(plan.getModelElement().getName());
			}
		}
		
		@Plan(trigger=@Trigger(factadded="trigger"))
		class ContextPlan
		{
			@PlanContextCondition(beliefs="bel")
			boolean context()
			{
				return bel.get();
			}
			
			@PlanBody
			void body(IPlan plan)
			{
				bel.set(false);
				contextfut.setException(new RuntimeException("not aborted"));
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
			IComponentHandle	agent	= IBDIAgent.create(pojo);
			agent.scheduleStep(() -> pojo.trigger.add("go"));
			assertEquals(PlanConditionTestAgent.PrePlan1.class.getName(), pojo.prefut.get(TestHelper.TIMEOUT));
		}
		
		// Second plan
		{
			PlanConditionTestAgent	pojo	= new PlanConditionTestAgent();
			IComponentHandle	agent	= IBDIAgent.create(pojo);
			agent.scheduleStep(() ->
			{
				pojo.bel.set(false);
				pojo.trigger.add("go");			
			});
			assertEquals(PlanConditionTestAgent.PrePlan2.class.getName(), pojo.prefut.get(TestHelper.TIMEOUT));
		}
	}
	
	@Test
	void testPlanContextcondition()
	{
		PlanConditionTestAgent	pojo	= new PlanConditionTestAgent();
		IComponentHandle	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep(() -> pojo.trigger.add("go"));
		assertEquals("aborted", pojo.contextfut.get(TestHelper.TIMEOUT));
	}
}

