package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalAPLBuild;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.goal.APL;
import jadex.bdi.impl.goal.ICandidateInfo;
import jadex.bdi.impl.plan.IPlanBody;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;

/**
 *  Test manual APL building.
 */
public class GoalAPLBuildTest
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
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(agent.new APLBuildGoal())).get(TestHelper.TIMEOUT);
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
					return Collections.singletonList(new APL.MPlanCandidate("myplan", body));
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
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(agent.new APLBuildGoal())).get(TestHelper.TIMEOUT);
		assertNull(agent.executed.get(TestHelper.TIMEOUT));
	}

}
