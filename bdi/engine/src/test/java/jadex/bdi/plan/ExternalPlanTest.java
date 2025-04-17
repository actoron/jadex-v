package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.Trigger;
import jadex.common.SUtil;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;

/**
 *  Test using external classes as plans
 */
public class ExternalPlanTest
{
	@BDIAgent
	@Plan(impl=MyExtPlan.class, trigger=@Trigger(goals=ExtPlanAgent.TriggerGoal.class))
	static class ExtPlanAgent
	{
		Future<Void>	processed	= new Future<>();
		
		@Goal
		class TriggerGoal {}
	}		
	
	@Plan
	static class MyExtPlan
	{
		@PlanBody
		void body(ExtPlanAgent agent)
		{
			agent.processed.setResult(null);
		}
	}

	@Test
	public void	testExternalPlan()
	{
		ExtPlanAgent	agent	= new ExtPlanAgent();
		IComponentHandle	handle	= IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
		assertFalse(agent.processed.isDone());
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(agent.new TriggerGoal())).get(TestHelper.TIMEOUT);
		assertTrue(agent.processed.isDone());
	}

	
	@Plan(trigger=@Trigger(factadded="dummy"))
	static class BrokenExtPlan {}
	
	@BDIAgent
	@Plan(impl=BrokenExtPlan.class, trigger=@Trigger(factchanged="dummy"))
	static class BrokenExtPlanAgent
	{
		@Belief
		Val<String> dummy;
	}		

	@Test
	public void	testBrokenExternalPlan()
	{
		IComponentHandle	handle	= IComponentManager.get().create(new BrokenExtPlanAgent()).get(TestHelper.TIMEOUT);
		SUtil.runWithoutOutErr(() ->
			assertThrows(ComponentTerminatedException.class, () ->
				handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT)));
	}
}
