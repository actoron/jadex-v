package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Test using external classes as goals
 */
public class ExternalGoalTest
{
	@BDIAgent
	@Goal(impl=MyExtGoal.class)
	static class ExtGoalAgent
	{
		Future<Void>	processed	= new Future<>();
		
		@Plan(trigger=@Trigger(goals=MyExtGoal.class))
		void processGoal()
		{
			processed.setResult(null);
		}
	}		
	
	@Goal
	static class MyExtGoal {}

	@Test
	public void	testExternalGoal()
	{
		ExtGoalAgent	agent	= new ExtGoalAgent();
		IComponentHandle	handle	= IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
		assertFalse(agent.processed.isDone());
		handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new MyExtGoal())).get(TestHelper.TIMEOUT);
		assertTrue(agent.processed.isDone());
	}

}
