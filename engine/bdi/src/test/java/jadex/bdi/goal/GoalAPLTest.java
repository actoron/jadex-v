package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalAPLBuild;
import jadex.bdi.annotation.GoalSelectCandidate;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.goal.APL;
import jadex.bdi.impl.goal.ICandidateInfo;
import jadex.bdi.impl.plan.IPlanBody;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Test manual APL building.
 */
public class GoalAPLTest
{
	@Test
	public void	testPojoGoalAPLBuild()
	{
		@BDIAgent
		class APLBuildAgent
		{
			Future<Void>	executed	= new Future<>();
			
			@Goal
			class APLBuildGoal
			{
				@GoalAPLBuild
				List<MyPlan>	buildAPL()
				{
					return Collections.singletonList(new MyPlan());
				}
			}
			
			@Plan
			class MyPlan
			{
				@PlanBody
				void body()
				{
					executed.setResult(null);
				}
			}
		}
		
		APLBuildAgent	agent	= new APLBuildAgent();
		IComponentHandle	handle	= IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(agent.new APLBuildGoal())).get(TestHelper.TIMEOUT);
		assertNull(agent.executed.get(TestHelper.TIMEOUT));
	}

	@Test
	public void	testCandidateInfoGoalAPLBuild()
	{
		@BDIAgent
		class APLBuildAgent
		{
			Future<Void>	executed	= new Future<>();
			
			@Goal
			class APLBuildGoal
			{
				@GoalAPLBuild
				List<ICandidateInfo>	buildAPL(IComponent comp)
				{
					IPlanBody	body	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getModel().getPlanBody(MyPlan.class);
					return Collections.singletonList(new APL.MPlanCandidate(Collections.singletonList(APLBuildAgent.class), "myplan", body));
				}
			}
			
			@Plan
			class MyPlan
			{
				@PlanBody
				void body()
				{
					executed.setResult(null);
				}
			}
		}
		
		APLBuildAgent	agent	= new APLBuildAgent();
		IComponentHandle	handle	= IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(agent.new APLBuildGoal())).get(TestHelper.TIMEOUT);
		assertNull(agent.executed.get(TestHelper.TIMEOUT));
	}

	@Test
	public void	testSelectCandidate()
	{
		@BDIAgent
		class SelectCandidate
		{
			@Belief
			List<String>	results	= new ArrayList<>();
			
			@Goal
			class SelectGoal
			{
				@GoalTargetCondition
				boolean	target()
				{
					return results.size()>=2;
				}
				
				@GoalSelectCandidate
				ICandidateInfo	select(List<ICandidateInfo>	candidates)
				{
					return results.isEmpty() ? candidates.get(1) : candidates.get(0);
				}
			}
			
			@Plan(trigger=@Trigger(goals=SelectGoal.class))
			void plan1()
			{
				results.add("result1");
			}
			
			@Plan(trigger=@Trigger(goals=SelectGoal.class))
			void plan2()
			{
				results.add("result2");
			}
		}
		
		SelectCandidate	agent	= new SelectCandidate();
		IComponentHandle	handle	= IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(agent.new SelectGoal())).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("result2", "result1"), agent.results);
	}
}
