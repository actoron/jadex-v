package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.GoalFailureException;
import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IPlan;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalContextCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanContextCondition;
import jadex.bdi.annotation.Trigger;
import jadex.core.IAsyncStep;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.injection.Val;

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
				@GoalContextCondition
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
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		assertEquals(1, executions.size());
//		System.out.println();

	}


	@Test
	public void	testSubgoalFlickering()
	{
		List<String>	executions	= new ArrayList<>();
		
		@BDIAgent
		class ContextAgent
		{
			@Belief
			Val<Boolean>	context	= new Val<Boolean>(true);
			
			@Goal
			class TopGoal{}
			@Goal
			class SubGoal{}
			
			@Plan(trigger=@Trigger(goals=TopGoal.class))
			class	TopPlan
			{
				@PlanContextCondition
				boolean context()
				{
					return context.get();
				}
				
				@PlanBody
				void body(IExecutionFeature exe, IPlan plan)
				{
					// Schedule step to abort plan.
					exe.scheduleStep(() -> context.set(false));
					
					// Dispatch subgoal (schedules AdoptGoalAction)
					// and block until aborted
					plan.dispatchSubgoal(new SubGoal()).get();
				}
			}

			@Plan(trigger=@Trigger(goals=SubGoal.class))
			void	myPlan()
			{
//				System.out.println("executed");
				executions.add("executed");
				new Future<>().get();
			}
		}
		
		ContextAgent	pojo	= new ContextAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		assertThrows(GoalFailureException.class, () ->
			handle.scheduleAsyncStep((IAsyncStep<Void>)comp -> comp.getFeature(IBDIAgentFeature.class)
					.dispatchTopLevelGoal(pojo.new TopGoal())).get(TestHelper.TIMEOUT));
		assertEquals(0, executions.size());
	}

	public static void main(String[] args)
	{
		for(;;)
		{
//			System.out.println("Test run...");
			GoalFlickerTest	test	= new GoalFlickerTest();
			test.testContextFlickering();
			test.testSubgoalFlickering();
		}
	}
}
