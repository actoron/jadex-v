package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalParameter;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.IFuture;
import jadex.injection.Val;

/**
 *  Test goal has a target condition based on the state of a parameter.
 */
@BDIAgent
public class GoalParameterConditionTest
{
	@Goal(recur=true, recurdelay=10000)
	public class HelloGoal	implements Supplier<String>
	{
		@GoalParameter
		protected Val<String> text;
		
		@GoalTargetCondition
		public boolean checkTarget()
		{
			return "finished".equals(text.get());
		}
		
		public String get()
		{
			return text.get();
		}
	}
	
	@Test
	public void testGoalParameterCondition()
	{
		IComponentHandle self	= IComponentManager.get().create(this).get(TestHelper.TIMEOUT);
		HelloGoal	goal	= new HelloGoal();
		IFuture<String>	fut	= self.scheduleAsyncStep(agent -> agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal));
		
		// Allow some component steps to process the goal.
		self.scheduleStep(agent -> {return null;}).get(TestHelper.TIMEOUT);
		self.scheduleStep(agent -> {return null;}).get(TestHelper.TIMEOUT);
		self.scheduleStep(agent -> {return null;}).get(TestHelper.TIMEOUT);
		self.scheduleStep(agent -> {return null;}).get(TestHelper.TIMEOUT);
		
		assertFalse(fut.isDone());

		self.scheduleStep(agent -> {goal.text.set("finished"); return null;}).get(TestHelper.TIMEOUT);
		assertEquals("finished", fut.get(TestHelper.TIMEOUT));
	}
}
