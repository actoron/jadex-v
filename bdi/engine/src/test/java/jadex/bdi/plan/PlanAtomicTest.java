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
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;

/**
 *  Test plan pre- and context conditions
 */
public class PlanAtomicTest
{
	@BDIAgent
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
		IComponentHandle	agent	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		agent.scheduleStep(() -> pojo.trigger.add("go"));
		assertEquals("not aborted", pojo.atomicfut.get(TestHelper.TIMEOUT));
		assertEquals("aborted", pojo.contextfut.get(TestHelper.TIMEOUT));
	}
}

