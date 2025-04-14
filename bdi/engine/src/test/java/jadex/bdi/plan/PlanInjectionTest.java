package jadex.bdi.plan;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.injection.annotation.Inject;

/**
 *  Test value injection in plan fields and methods.
 */
public class PlanInjectionTest
{
	/**
	 *  Test that the context specific goal object can be injected.
	 */
	@Test
	public void	testGoalInjection()
	{
		@BDIAgent
		class PlanInjectionAgent
		{
			@Goal
			static class MyGoal {}
			
			// Test that goal from local context can be injected
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			static class MyPlan
			{
				@Inject
				MyGoal	thegoal;
			}
		}
		
		IComponentHandle	handle	= IComponentManager.get().create(new PlanInjectionAgent()).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new PlanInjectionAgent.MyGoal())).get(TestHelper.TIMEOUT);
	}
}

