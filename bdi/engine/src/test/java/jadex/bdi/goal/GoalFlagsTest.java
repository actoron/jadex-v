package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.IntermediateFuture;

public class GoalFlagsTest
{
	@Test
	public void	testRecur()
	{
		@BDIAgent
		class RecurAgent
		{
			IntermediateFuture<Integer>	fut	= new IntermediateFuture<>();
			
			@Belief
			Val<Integer>	cnt	= new Val<>(0);
			
			@Goal(recur=true)
			class MyGoal
			{
				@GoalTargetCondition(beliefs="cnt")
				boolean	target()
				{
					return cnt.get()>3;
				}
			}
			

			@Plan(trigger=@Trigger(goals=MyGoal.class))
			void	myPlan()
			{
				cnt.set(cnt.get()+1);
				fut.addIntermediateResult(cnt.get());
			}
			
		}
		RecurAgent	pojo	= new RecurAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal())).get(TestHelper.TIMEOUT);
		assertEquals(1, pojo.fut.getNextIntermediateResult());
		assertEquals(2, pojo.fut.getNextIntermediateResult());
		assertEquals(3, pojo.fut.getNextIntermediateResult());
	}
}
