package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bdi.GoalFailureException;
import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.PlanFailureException;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.ExcludeMode;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.IFuture;
import jadex.future.IntermediateFuture;
import jadex.injection.Val;

/**
 *  Test various goal flags.
 */
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
				@GoalTargetCondition
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
		handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal())).get(TestHelper.TIMEOUT);
		assertEquals(1, pojo.fut.getNextIntermediateResult());
		assertEquals(2, pojo.fut.getNextIntermediateResult());
		assertEquals(3, pojo.fut.getNextIntermediateResult());
	}

	@Test
	public void	testNotOrsuccess()
	{
		@BDIAgent
		class OrsuccessAgent
		{
			IntermediateFuture<Integer>	fut	= new IntermediateFuture<>();
			
			@Goal(orsuccess=false)
			class MyGoal {}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			void	myPlan1()
			{
				fut.addIntermediateResult(1);
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			void	myPlan2()
			{
				fut.addIntermediateResult(2);
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			void	myPlan3()
			{
				fut.addIntermediateResult(3);
			}
		}
		
		OrsuccessAgent	pojo	= new OrsuccessAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal())).get(TestHelper.TIMEOUT);
		assertEquals(1, pojo.fut.getNextIntermediateResult());
		assertEquals(2, pojo.fut.getNextIntermediateResult());
		assertEquals(3, pojo.fut.getNextIntermediateResult());
	}

	@Test
	public void	testRecurDelay()
	{
		@BDIAgent
		class RecurDelayAgent
		{
			IntermediateFuture<Integer>	fut	= new IntermediateFuture<>();
			
			@Goal(recur=true, recurdelay=500)
			class MyGoal {}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			void	myPlan1()
			{
				fut.addIntermediateResult(1);
			}
		}
		
		RecurDelayAgent	pojo	= new RecurDelayAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal()));
		assertEquals(1, pojo.fut.getNextIntermediateResult());
		long	before	= System.nanoTime();
		assertEquals(1, pojo.fut.getNextIntermediateResult());
		long	after	= System.nanoTime();
		assertTrue(after-before>400000000, "Waited: "+(after-before));
		handle.terminate().get(TestHelper.TIMEOUT);
	}
	
	@Test
	public void	testExcludeNever()
	{
		@BDIAgent
		class ExcludeNeverAgent
		{
			@Belief
			List<String>	list	= new ArrayList<>();
			
			@Goal(excludemode=ExcludeMode.Never)
			class MyGoal
			{
				@GoalTargetCondition
				boolean	cond()
				{
					return list.size()>1;
				}
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			void	myPlan1()
			{
				list.add("value");
			}
		}
		
		ExcludeNeverAgent	pojo	= new ExcludeNeverAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal())).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("value", "value"), pojo.list);
	}
	
	@Test
	public void	testExcludeWhenFailed()
	{
		@BDIAgent
		class ExcludeWhenFailedAgent
		{
			@Belief
			List<String>	list1	= new ArrayList<>();
			
			@Belief
			List<String>	list2	= new ArrayList<>();
			
			@Goal(excludemode=ExcludeMode.WhenFailed)
			class MyGoal1
			{
				@GoalTargetCondition
				boolean	cond()
				{
					return list1.size()>1;
				}
			}
			
			@Goal(excludemode=ExcludeMode.WhenFailed)
			class MyGoal2
			{
				@GoalTargetCondition
				boolean	cond()
				{
					return list2.size()>1;
				}
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal1.class))
			void	myPlan1()
			{
				list1.add("value");
				throw new PlanFailureException();
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal2.class))
			void	myPlan2()
			{
				list2.add("value");
			}
		}
		
		ExcludeWhenFailedAgent	pojo	= new ExcludeWhenFailedAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		
		// Check that first goal with failing plan fails.
		assertThrows(PlanFailureException.class, 
			() -> handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>) comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal1())).get(TestHelper.TIMEOUT));
		
		// Check that second goal with passing plan succeeds.
		handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal2())).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("value", "value"), pojo.list2);
	}

	
	@Test
	public void	testExcludeWhenSucceeded()
	{
		@BDIAgent
		class ExcludeWhenSucceededAgent
		{
			@Belief
			List<String>	list1	= new ArrayList<>();
			
			@Belief
			List<String>	list2	= new ArrayList<>();
			
			@Goal(excludemode=ExcludeMode.WhenSucceeded)
			class MyGoal1
			{
				@GoalTargetCondition
				boolean	cond()
				{
					return list1.size()>1;
				}
			}
			
			@Goal(excludemode=ExcludeMode.WhenSucceeded)
			class MyGoal2
			{
				@GoalTargetCondition
				boolean	cond()
				{
					return list2.size()>1;
				}
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal1.class))
			void	myPlan1()
			{
				list1.add("value");
				throw new PlanFailureException();
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal2.class))
			void	myPlan2()
			{
				list2.add("value");
			}
		}
		
		ExcludeWhenSucceededAgent	pojo	= new ExcludeWhenSucceededAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		
		// Check that first goal with failing plan succeeds.
		handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal1())).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("value", "value"), pojo.list1);
		
		// Check that second goal with passing plan fails.
		assertThrows(GoalFailureException.class,
			() -> handle.scheduleAsyncStep((INoCopyStep<IFuture<Void>>)comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.new MyGoal2())).get(TestHelper.TIMEOUT));
	}
}
