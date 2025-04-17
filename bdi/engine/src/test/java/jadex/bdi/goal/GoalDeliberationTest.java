package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanPassed;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;

/**
 *  Test goal deliberation.
 */
public class GoalDeliberationTest
{
	@Test
	public void	testTypeInhibition()
	{
		@BDIAgent
		class GoaltypeDeliberationAgent
		{
			Future<Void>	waiting	= new Future<>();
			Future<Void>	aborted	= new Future<>();
			Future<Void>	passed	= new Future<>();
			
			@Goal()
			class Inhibited	{}
			
			@Goal(deliberation=@Deliberation(inhibits=Inhibited.class))
			class Inhibitor {}
			
			@Plan(trigger=@Trigger(goals=Inhibitor.class))
			void dummy() {}
			
			@Plan(trigger=@Trigger(goals=Inhibited.class))
			class ProcessInhibited
			{
				@PlanBody
				void	body()
				{
					// Plan is called twice: before and after goal inhibition
					if(!waiting.isDone())
					{
						waiting.setResult(null);
						
						// Block forever.
						new Future<Void>().get();
					}
				}
				
				@PlanPassed
				void	passed()
				{
					passed.setResult(null);
				}
				
				@PlanAborted
				void	aborted()
				{
					aborted.setResult(null);
				}
			}
		}
		
		GoaltypeDeliberationAgent pojo	= new GoaltypeDeliberationAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new Inhibited()));
		assertNull(pojo.waiting.get(TestHelper.TIMEOUT));
		assertFalse(pojo.passed.isDone());
		assertFalse(pojo.aborted.isDone());
		
		// Add other to goal abort plan.
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new Inhibitor())).get(TestHelper.TIMEOUT);
		assertNull(pojo.aborted.get(TestHelper.TIMEOUT));
		
		// Check passed to see if goal gets reactivated after the other succeeds
		assertNull(pojo.passed.get(TestHelper.TIMEOUT));
	}
}
