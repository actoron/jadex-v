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
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.IBDIAgent;
import jadex.bdi.runtime.IPlan;
import jadex.bdi.runtime.Val;
import jadex.core.IExternalAccess;
import jadex.future.Future;
import jadex.micro.annotation.Agent;

/**
 *  Test plan pre- and context conditions
 */
public class PlanAtomicTest
{
	@Agent(type="bdip")
	static class PlanAtomicTestAgent
	{
		@Belief
		Val<Boolean>	bel	= new Val<>(true);

		@Belief
		List<String>	trigger	= new ArrayList<>();

		Future<String>	contextfut	= new Future<>();
		Future<String>	atomicfut	= new Future<>();
		
		@Plan(trigger=@Trigger(factadded="trigger"))
		class AtomicPlan
		{
			@PlanContextCondition(beliefs="bel")
			boolean context()
			{
				return bel.get();
			}
			
			@PlanBody
			void body(IPlan plan)
			{
				plan.startAtomic();
				bel.set(false);
				atomicfut.setResultIfUndone("not aborted");
				plan.endAtomic();
			}
			
			@PlanAborted
			void aborted()
			{
				contextfut.setResultIfUndone("aborted");				
			}
		}
	}
	
	@Test
	void testAtomicPlan()
	{
		PlanAtomicTestAgent	pojo	= new PlanAtomicTestAgent();
		IExternalAccess	agent	= IBDIAgent.create(pojo);
		agent.scheduleStep(() -> pojo.trigger.add("go"));
		assertEquals("not aborted", pojo.atomicfut.get(TestHelper.TIMEOUT));
		assertEquals("aborted", pojo.contextfut.get(TestHelper.TIMEOUT));
	}
}

