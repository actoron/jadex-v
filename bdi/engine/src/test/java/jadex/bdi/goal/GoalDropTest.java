package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bdi.GoalDroppedException;
import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Tets goal drop functionality.
 */
public class GoalDropTest
{
	@Test
	public void	testManualDrop()
	{
		@BDIAgent
		class GoalDropAgent
		{
			Future<Void>	aborted	= new Future<>();
			
			@Goal
			class DropGoal {}
			
			@Plan(trigger=@Trigger(goals=DropGoal.class))
			class blockPlan
			{
				@PlanBody
				void body()
				{
					new Future<>().get();
				}
				
				@PlanAborted
				void abort()
				{
					aborted.setResult(null);
				}
			}
		}
		
		GoalDropAgent	pojo	= new GoalDropAgent();
		GoalDropAgent.DropGoal	pojogoal	= pojo.new DropGoal();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		IFuture<Void>	goalfut	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojogoal));
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra step to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertFalse(goalfut.isDone()); // Should be processing
		assertFalse(pojo.aborted.isDone());
		
		handle.scheduleStep(comp -> {comp.getFeature(IBDIAgentFeature.class).dropGoal(pojogoal); return null;}).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);	// Extra steps to allow goal processing to be finished
		assertTrue(goalfut.isDone()); // Should be failed.
		assertThrows(GoalDroppedException.class, () -> goalfut.get(TestHelper.TIMEOUT));
		assertNull(pojo.aborted.get(TestHelper.TIMEOUT));
	}
}
