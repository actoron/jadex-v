package jadex.bdi.plan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IPlan;
import jadex.bdi.PlanFailureException;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.Trigger;
import jadex.common.TimeoutException;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.ITerminableStep;
import jadex.core.IThrowingFunction;
import jadex.future.Future;
import jadex.future.FutureTerminatedException;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;

/**
 *  Test subgoal handling of plans.
 */
public class PlanSubgoalTest
{
	@Test
	public void	testSubgoalAbort()
	{
		@BDIAgent
		class SubgoalAgent
		{
			IFuture<Void>	subfut;
			Future<Void>	started	= new Future<>();
			Future<Void>	aborted	= new Future<>();
			
			@Goal
			class TopGoal{}
			
			@Goal
			class Subgoal{}
			
			@Plan(trigger=@Trigger(goals=TopGoal.class))
			void	handleTopGoal(IPlan plan)
			{
				subfut	= plan.dispatchSubgoal(new Subgoal());
				subfut.get(TestHelper.TIMEOUT);
			}
			
			@Plan(trigger=@Trigger(goals=Subgoal.class))
			class Subplan
			{
				@PlanBody
				void	handleSubgoal()
				{
					started.setResult(null);
					new Future<>().get(TestHelper.TIMEOUT);
				}
				
				@PlanAborted
				void aborted()
				{
					aborted.setResult(null);			
				}
			}
		}
		
		SubgoalAgent	agent	= new SubgoalAgent();
		IComponentHandle	handle	= IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		
		// Dispatch top-level goal and check if sub plan is started.
		ITerminableFuture<Void>	topfut	= (ITerminableFuture<Void>) handle.scheduleAsyncStep(
			(ITerminableStep<Void>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(agent.new TopGoal()));
		agent.started.get(TestHelper.TIMEOUT);
		assertFalse(agent.aborted.isDone());
		
		// Drop top-level goal and check if sub plan is aborted.
		@SuppressWarnings("serial")
		class MyException extends RuntimeException{};
		handle.scheduleStep(() -> topfut.terminate(new MyException())).get(TestHelper.TIMEOUT);
		agent.aborted.get(TestHelper.TIMEOUT);
		
//		assertThrows(GoalDroppedException.class, () -> agent.subfut.get(TestHelper.TIMEOUT));
		assertThrows(FutureTerminatedException.class, () -> agent.subfut.get(TestHelper.TIMEOUT));
		assertThrows(MyException.class, () -> topfut.get(TestHelper.TIMEOUT));
	}
	
	
	@Test
	public void	testSubgoalTimeout()
	{
		@BDIAgent
		class SubgoalAgent
		{
			IFuture<Void>	subfut;
			Future<Void>	started	= new Future<>();
			Future<Void>	aborted	= new Future<>();
			
			@Goal
			class TopGoal{}
			
			@Goal
			class Subgoal{}
			
			@Plan(trigger=@Trigger(goals=TopGoal.class))
			void	handleTopGoal(IPlan plan)
			{
				try
				{
					subfut	= plan.dispatchSubgoal(new Subgoal());
					subfut.get(50);
				}
				catch(TimeoutException te)
				{
					throw new PlanFailureException();
				}
			}
			
			@Plan(trigger=@Trigger(goals=Subgoal.class))
			class Subplan
			{
				@PlanBody
				void	handleSubgoal()
				{
					started.setResult(null);
					new Future<>().get(TestHelper.TIMEOUT);
				}
				
				@PlanAborted
				void aborted()
				{
					aborted.setResult(null);			
				}
			}
		}
		
		SubgoalAgent	agent	= new SubgoalAgent();
		IComponentHandle	handle	= IComponentManager.get().create(agent).get(TestHelper.TIMEOUT);
		handle.scheduleStep(() -> null).get(TestHelper.TIMEOUT);
		
		// Dispatch top-level goal and check if sub plan is started.
		ITerminableFuture<Void>	topfut	= (ITerminableFuture<Void>) handle.scheduleAsyncStep(new IThrowingFunction<IComponent, IFuture<Void>>()
		{
			public ITerminableFuture<Void> apply(IComponent comp)
			{
				return comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(agent.new TopGoal());
			}
		});
		agent.started.get(TestHelper.TIMEOUT);
		assertFalse(agent.aborted.isDone());
		
		// Sub plan should be aborted after top-plan timeout
		agent.aborted.get(TestHelper.TIMEOUT);
		
//		assertThrows(GoalDroppedException.class, () -> agent.subfut.get(TestHelper.TIMEOUT));
		assertThrows(FutureTerminatedException.class, () -> agent.subfut.get(TestHelper.TIMEOUT));
		assertThrows(PlanFailureException.class, () -> topfut.get(TestHelper.TIMEOUT));
	}
}
