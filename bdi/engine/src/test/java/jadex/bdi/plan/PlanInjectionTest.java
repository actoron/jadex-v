package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanPassed;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;

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
	
	/**
	 *  Test correct invocation of onstart/end.
	 */
	@Test
	public void	testOnStartEnd()
	{
		@BDIAgent
		class OnStartEndAgent
		{
			Future<Void>	onstart	= new Future<>();
			Future<Void>	body	= new Future<>();
			Future<Void>	passed	= new Future<>();
			Future<Void>	onend	= new Future<>();
			
			@Goal
			static class MyGoal {}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			class MyPlan
			{
				@OnStart
				void onstart()
				{
					assertFalse(body.isDone());
					assertFalse(passed.isDone());
					assertFalse(onend.isDone());
					onstart.setResult(null);
				}

				@PlanBody
				void body()
				{
					assertTrue(onstart.isDone());
					assertFalse(passed.isDone());
					assertFalse(onend.isDone());
					body.setResult(null);
				}

				@PlanPassed
				void passed()
				{
					assertTrue(onstart.isDone());
					assertTrue(body.isDone());
					assertFalse(onend.isDone());
					passed.setResult(null);
				}

				@OnEnd
				void onend()
				{
					assertTrue(onstart.isDone());
					assertTrue(body.isDone());
					assertTrue(passed.isDone());
					onend.setResult(null);
				}
			}
		}
		OnStartEndAgent	pojo	= new OnStartEndAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new OnStartEndAgent.MyGoal())).get(TestHelper.TIMEOUT);
		pojo.onstart.get(TestHelper.TIMEOUT);
		pojo.body.get(TestHelper.TIMEOUT);
		pojo.passed.get(TestHelper.TIMEOUT);
		pojo.onend.get(TestHelper.TIMEOUT);
	}
}

