package jadex.bdi.goal;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IPlan;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.IFuture;

/**
 *  Test various goal conditions
 */
public class GoalResultTest
{
	@Test
	public void	testGoalResult()
	{
		@BDIAgent
		class GoalResultAgent
		{
			@Goal
			class ResultGoal	implements Supplier<String>
			{
				String	result;
				
				@Override
				public String get()
				{
					return result;
				}
			}
			
			@Plan(trigger=@Trigger(goals=ResultGoal.class))
			class myPlan
			{
				@PlanBody
				void body(ResultGoal goal)
				{
					goal.result	= "result";
				}
			}
		}
		
		GoalResultAgent	pojo	= new GoalResultAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		IFuture<String>	goalfut	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new ResultGoal()));
		assertEquals("result", goalfut.get(TestHelper.TIMEOUT));
	}
	
	@Test
	public void	testSubgoalResult()
	{
		@BDIAgent
		class SubgoalResultAgent
		{
			IFuture<String>	resultfut;
			
			@Goal
			class ResultGoal	implements Supplier<String>
			{
				String	result;
				
				@Override
				public String get()
				{
					return result;
				}
			}
			
			@Plan(trigger=@Trigger(goals=ResultGoal.class))
			class myPlan
			{
				@PlanBody
				void body(ResultGoal goal)
				{
					goal.result	= "result";
				}
			}
			
			@Goal
			class TopGoal {}
			
			@Plan(trigger=@Trigger(goals=TopGoal.class))
			void topPlan(IPlan plan)
			{
				resultfut	= plan.dispatchSubgoal(new ResultGoal());
				
				// Need to wait otherwise subgoal is dropped when plan is passed.
				resultfut.get(TestHelper.TIMEOUT);
			}
		}
		
		SubgoalResultAgent	pojo	= new SubgoalResultAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		IFuture<Void>	goalfut	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new TopGoal()));
		assertNull(goalfut.get(TestHelper.TIMEOUT));
		assertEquals("result", pojo.resultfut.get(TestHelper.TIMEOUT));
	}
}
