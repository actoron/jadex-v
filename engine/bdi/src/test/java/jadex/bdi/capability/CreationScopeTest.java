package jadex.bdi.capability;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IPlan;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.Val;
import jadex.injection.annotation.Inject;

/**
 *  Test if correct outer pojos are used.
 */
public class CreationScopeTest
{
	@Test
	public void	testGoalCreation()
	{
		class MyCapa
		{
			@Goal
			class MyGoal	implements Supplier<MyCapa>
			{
				// Can only be injected when parent pojo of goal is correct.
				@Inject
				MyCapa	mycapa;
				
				@Override
				public MyCapa get()
				{
					return mycapa;
				}
			}
		}
		
		@BDIAgent
		class MyAgent
		{
			@Capability
			MyCapa	mycapa	= new MyCapa(); 
		}
		
		MyAgent	pojo	= new MyAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		MyCapa	capa	= handle.scheduleAsyncStep((INoCopyStep<IFuture<MyCapa>>)comp -> comp.getFeature(IBDIAgentFeature.class)
			.dispatchTopLevelGoal(pojo.mycapa.new MyGoal())).get(TestHelper.TIMEOUT);
		assertSame(pojo.mycapa, capa);
	}
	
	
	@Test
	public void	testCrossCapabilityPlanTriggering()
	{
		class MyCapa1
		{
			@Goal
			class MyGoal{}
		}
		
		class MyCapa2
		{
			Future<MyCapa2>	result	= new Future<>();
			
			@Plan(trigger=@Trigger(goals=MyCapa1.MyGoal.class))
			void	myPlan(MyCapa2 capa)
			{
				result.setResult(capa);
			}
		}
		
		@BDIAgent
		class MyAgent
		{
			@Capability
			MyCapa1	mycapa1	= new MyCapa1();
			
			@Capability
			MyCapa2	mycapa2	= new MyCapa2(); 
		}
		
		MyAgent	pojo	= new MyAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class)
			.dispatchTopLevelGoal(pojo.mycapa1.new MyGoal())).get(TestHelper.TIMEOUT);
		assertSame(pojo.mycapa2, pojo.mycapa2.result.get(TestHelper.TIMEOUT));
	}

	class MyCrossGoalCapa1
	{
		@Goal
		class MyGoal	implements Supplier<MyCrossGoalCapa1>
		{
			// Can only be injected when parent pojo of goal is correct.
			@Inject
			MyCrossGoalCapa1	mycapa;
			
			@Override
			public MyCrossGoalCapa1 get()
			{
				return mycapa;
			}
		}
	}
	
	class MyCrossGoalCapa2
	{
		@Belief
		Val<Boolean>	trigger	= new Val<>(false);
		
		Future<MyCrossGoalCapa1>	result	= new Future<>();
		
		@Plan(trigger=@Trigger(factchanged="trigger"))
		void	myPlan(IPlan plan, MyCrossGoalAgent agent)
		{
			MyCrossGoalCapa1	capa	= plan.dispatchSubgoal(agent.mycapa1.new MyGoal()).get();
			result.setResult(capa);
		}
	}
	
	@BDIAgent
	class MyCrossGoalAgent
	{
		@Capability
		MyCrossGoalCapa1	mycapa1	= new MyCrossGoalCapa1();
		
		@Capability
		MyCrossGoalCapa2	mycapa2	= new MyCrossGoalCapa2(); 
	}

	@Test
	public void	testCrossCapabilityGoalCreation()
	{		
		MyCrossGoalAgent	pojo	= new MyCrossGoalAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleStep(comp -> {pojo.mycapa2.trigger.set(true); return null;}).get(TestHelper.TIMEOUT);
		assertSame(pojo.mycapa1, pojo.mycapa2.result.get(TestHelper.TIMEOUT));
	}
}
