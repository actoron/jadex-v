package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalInhibit;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanPassed;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IntermediateFuture;

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


	@Test
	public void	testCardinalityOne()
	{
		@BDIAgent
		class CardinalityOneAgent
		{
			int	plancnt	= 0;
			Future<Void>	block	= new Future<>();
			IntermediateFuture<Void>	processing	= new IntermediateFuture<>();
			
			@Goal(deliberation=@Deliberation(cardinalityone=true))
			class Inhibit {}
			
			@Plan(trigger=@Trigger(goals=Inhibit.class))
			class ProcessInhibit
			{
				@PlanBody
				void	body()
				{
					plancnt++;
					processing.addIntermediateResult(null);
					
					// First plan: Blocks until released.
					block.get();
				}
			}
		}
		
		CardinalityOneAgent pojo	= new CardinalityOneAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new Inhibit()));
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new Inhibit()));
		pojo.processing.getNextIntermediateResult(TestHelper.TIMEOUT);
		assertEquals(1, pojo.plancnt);
		
		// Release plan to process second goal.
		pojo.block.setResult(null);
		pojo.processing.getNextIntermediateResult(TestHelper.TIMEOUT);
		assertEquals(2, pojo.plancnt);
	}


	@Test
	public void	testInstanceInhibit()
	{
		@BDIAgent
		class InstanceInhibitAgent
		{
			IntermediateFuture<Integer>	results	= new IntermediateFuture<>();
			
			@Goal
			record MyGoal(int value)
			{
				@GoalInhibit(MyGoal.class)
				boolean inhibits(MyGoal other)
				{
					return this.value()>other.value();
				}
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			void	myPlan(MyGoal goal)
			{
				results.addIntermediateResult(goal.value());
			}
		}
		
		InstanceInhibitAgent pojo	= new InstanceInhibitAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		
		handle.scheduleStep(comp -> 
		{
			comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new InstanceInhibitAgent.MyGoal(1));
			comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new InstanceInhibitAgent.MyGoal(2));
			comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new InstanceInhibitAgent.MyGoal(3));
			return null;
		}).get(TestHelper.TIMEOUT);
		
		assertEquals(3, pojo.results.getNextIntermediateResult(TestHelper.TIMEOUT));
		assertEquals(2, pojo.results.getNextIntermediateResult(TestHelper.TIMEOUT));
		assertEquals(1, pojo.results.getNextIntermediateResult(TestHelper.TIMEOUT));
	}
	
	
	@Test
	public void	testInstanceInhibitSwitch()
	{
		@BDIAgent
		class InstanceInhibitSwitchAgent
		{
			Future<Void>	started	= new Future<>();
			Future<Void>	aborted	= new Future<>();
			
			@Goal(deliberation=@Deliberation(cardinalityone=true))
			record MyGoal(int value)
			{
				@GoalInhibit(MyGoal.class)
				boolean inhibits(MyGoal other)
				{
					return this.value()>other.value();
				}
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			class BlockPlan
			{
				@PlanBody
				void body()
				{
					started.setResultIfUndone(null);
					new Future<>().get();
				}
				
				@PlanAborted
				void abort()
				{
					aborted.setResult(null);
				}
			}
		}
		
		InstanceInhibitSwitchAgent pojo	= new InstanceInhibitSwitchAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new InstanceInhibitSwitchAgent.MyGoal(1)));
		
		// Plan is started but not aborted.
		assertNull(pojo.started.get(TestHelper.TIMEOUT));
		assertFalse(pojo.aborted.isDone());

		// Create second goal to disable first.
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new InstanceInhibitSwitchAgent.MyGoal(2)));
		assertNull(pojo.aborted.get(TestHelper.TIMEOUT));
	}
}
