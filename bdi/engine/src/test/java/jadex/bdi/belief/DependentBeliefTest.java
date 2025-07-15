package jadex.bdi.belief;

import org.junit.jupiter.api.Test;

import jadex.bdi.TestHelper;
import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.core.IComponentManager;

/**
 *  Test that belief dependencies are automatically detected.
 */
public class DependentBeliefTest
{
	@BDIAgent
	static class DependentBeliefAgent
	{
		@Belief
		Val<Boolean> belief = new Val<>(false);
		
		boolean agentGetBelief()
		{
			return belief.get();
		}
		
		// TODO: dynamic belief
		@Belief
		Val<Boolean> dynamicBelief = new Val<>(() -> belief.get());
		
		@Goal
		class MyGoal
		{
			@GoalTargetCondition
			public boolean testBeliefAccess()
			{
				return belief.get();
			}
			
			@GoalTargetCondition
			public boolean testAgentMethod()
			{
				return agentGetBelief();
			}
			
			@GoalTargetCondition
			public boolean testGoalMethod()
			{
				return goalGetBelief();
			}
			
			boolean goalGetBelief()
			{
				return belief.get();
			}
		}
	}

	@Test
	public void testDependentBelief()
	{
		// Agent creation will fail if the belief dependencies are not detected.
		DependentBeliefAgent agent = new DependentBeliefAgent();
		IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
	}

	@Test
	public void testSubAgent()
	{
		// Agent creation will fail if the belief dependencies are not detected.
		DependentBeliefAgent agent = new DependentBeliefAgent() {};
		IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
	}
}
