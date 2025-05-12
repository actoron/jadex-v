package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalContextCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;

/**
 *  Test means-end reasoning (i.e. plan action removal)
 *  on flickering goals.
 */
public class GoalFlickerTest
{
	@Test
	public void	testContextFlickering()
	{
		List<String>	executions	= new ArrayList<>();
		
		@BDIAgent
		class ContextAgent
		{
			@Belief
			Val<Boolean>	context	= new Val<Boolean>(false);
			
			@Goal
			class FlickerGoal
			{
				@GoalContextCondition(beliefs="context")
				boolean	context()
				{
					return context.get();
				}
			}
			
			@Plan(trigger=@Trigger(goals=FlickerGoal.class))
			void	myPlan()
			{
//				System.out.println("executed");
				executions.add("executed");
				new Future<>().get();
			}
		}
		
		ContextAgent	pojo	= new ContextAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		
		// Check that no plan is started with initially false goal context condition
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new FlickerGoal()));
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		assertEquals(0, executions.size());
//		System.out.println();
		
		// Check that only one plan executes when toggle to true twice without step in between.
		executions.clear();
		handle.scheduleStep(() ->
		{
			pojo.context.set(true);
			pojo.context.set(false);
			pojo.context.set(true);
			return null;
		}).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		assertEquals(1, executions.size());
//		System.out.println();
		
		// Check that only one plan executes when plan aborted
		// (calls planFinished() and may trigger next candidate)
		// and toggled to true (executes new plan).
		executions.clear();
		handle.scheduleStep(() ->
		{
			pojo.context.set(false);
			pojo.context.set(true);
			return null;
		}).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		assertEquals(1, executions.size());
//		System.out.println();

	}
}
