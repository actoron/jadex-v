package jadex.bdi.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IBeliefListener;
import jadex.bdi.ICapability;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanContextCondition;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.Dyn;
import jadex.injection.Val;
import jadex.injection.annotation.Inject;
import jadex.rules.eca.ChangeInfo;

/**
 *  Check scoped belief naming.
 */
public class TestBeliefNames
{
	@Test
	public void testGlobalBeliefListener()
	{
		class MyCapa
		{
			@Belief
			Val<String>	belief	= new Val<>("initial");
		}
		
		@BDIAgent
		class MyAgent
		{
			@Capability
			MyCapa	capa	= new MyCapa();
		}
		
		Future<String>	result	= new Future<>();
		MyAgent	pojo	= new MyAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleStep(comp ->
		{
			comp.getFeature(IBDIAgentFeature.class).addBeliefListener("capa.belief",
				new IBeliefListener<String>()
			{
				@Override
				public void factChanged(ChangeInfo<String> change)
				{
					result.setResult(change.getValue());
				}
			});
			pojo.capa.belief.set("value");
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals("value", result.get(TestHelper.TIMEOUT));
	}
	
	@Test
	public void testLocalBeliefListener()
	{
		class MyCapa
		{
			@Belief
			Val<String>	belief	= new Val<>("initial");
			
			@Inject
			ICapability	capa;
		}
		
		@BDIAgent
		class MyAgent
		{
			@Capability
			MyCapa	capa	= new MyCapa();
		}
		
		Future<String>	result	= new Future<>();
		MyAgent	pojo	= new MyAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleStep(comp ->
		{
			pojo.capa.capa.addBeliefListener("belief",
				new IBeliefListener<String>()
			{
				@Override
				public void factChanged(ChangeInfo<String> change)
				{
					result.setResult(change.getValue());
				}
			});
			pojo.capa.belief.set("value");
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals("value", result.get(TestHelper.TIMEOUT));
	}
	
	@Test
	public void testDependentBelief()
	{
		class MyCapa
		{
			@Belief
			Val<String>	belief	= new Val<>("initial");
			
			@Belief
			Dyn<String>	dep	= new Dyn<>(() -> belief.get());
		}
		
		@BDIAgent
		class MyAgent
		{
			@Capability
			MyCapa	capa	= new MyCapa();
		}
		
		Future<String>	result	= new Future<>();
		MyAgent	pojo	= new MyAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleStep(comp ->
		{
			comp.getFeature(IBDIAgentFeature.class).addBeliefListener("capa.dep",
				new IBeliefListener<String>()
			{
				@Override
				public void factChanged(ChangeInfo<String> change)
				{
					result.setResult(change.getValue());
				}
			});
			pojo.capa.belief.set("value");
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals("value", result.get(TestHelper.TIMEOUT));
	}
	
	@Test
	public void testPlanTrigger()
	{
		class MyCapa
		{
			@Belief
			Val<String>	belief	= new Val<>("initial");
			
			Future<String>	result	= new Future<>();
			
			@Plan(trigger=@Trigger(factchanged="belief"))
			void plan(String value)
			{
				result.setResult(value);
			}
		}
		
		@BDIAgent
		class MyAgent
		{
			@Capability
			MyCapa	capa	= new MyCapa();
		}
		
		MyAgent	pojo	= new MyAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleStep(comp ->
		{
			pojo.capa.belief.set("value");
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals("value", pojo.capa.result.get(TestHelper.TIMEOUT));
	}
	
	@Test
	public void testGoalCreation()
	{
		class MyCapa
		{
			@Belief
			Val<String>	belief	= new Val<>("initial");
			
			Future<String>	result	= new Future<>();
			
			@Goal
			class MyGoal
			{
				String	value;
				
				@GoalCreationCondition(factchanged="belief")
				public MyGoal(String value)
				{
					this.value	= value;
				}
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			void plan(MyGoal goal)
			{
				result.setResult(goal.value);
			}
		}
		
		@BDIAgent
		class MyAgent
		{
			@Capability
			MyCapa	capa	= new MyCapa();
		}
		
		MyAgent	pojo	= new MyAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleStep(comp ->
		{
			pojo.capa.belief.set("value");
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals("value", pojo.capa.result.get(TestHelper.TIMEOUT));
	}
	
	@Test
	public void testGoalTarget()
	{
		class MyCapa
		{
			@Belief
			Val<String>	belief	= new Val<>("initial");
			
			@Goal
			class MyGoal
			{
				@GoalTargetCondition
				boolean	target()
				{
					return belief.get().equals("value");
				}
			}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			void plan(MyGoal goal)
			{
				new Future<>().get();
			}
		}
		
		@BDIAgent
		class MyAgent
		{
			@Capability
			MyCapa	capa	= new MyCapa();
		}
		
		MyAgent	pojo	= new MyAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		IFuture<Void>	goalfut	= handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.capa.new MyGoal()));
		handle.scheduleStep(comp ->
		{
			pojo.capa.belief.set("value");
			return null;
		}).get(TestHelper.TIMEOUT);
		assertNull(goalfut.get(TestHelper.TIMEOUT));
	}
	
	@Test
	public void testPlanContext()
	{
		class MyCapa
		{
			@Belief
			Val<String>	belief	= new Val<>("initial");
			
			Future<Boolean>	running	= new Future<>();
			Future<String>	result	= new Future<>();
			
			@Goal
			class MyGoal {}
			
			@Plan(trigger=@Trigger(goals=MyGoal.class))
			class MyPlan
			{
				@PlanContextCondition
				boolean context()
				{
					return "initial".equals(belief.get());
				}
				
				@PlanBody
				void plan()
				{
					running.setResult(true);
					new Future<>().get();
				}
				
				@PlanAborted
				void	abort()
				{
					result.setResult(belief.get());
				}
			}
		}
		
		@BDIAgent
		class MyAgent
		{
			@Capability
			MyCapa	capa	= new MyCapa();
		}
		
		MyAgent	pojo	= new MyAgent();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		handle.scheduleAsyncStep(comp -> comp.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(pojo.capa.new MyGoal()));
		assertTrue(pojo.capa.running.get(TestHelper.TIMEOUT));
		handle.scheduleStep(comp ->
		{
			pojo.capa.belief.set("value");
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals("value", pojo.capa.result.get(TestHelper.TIMEOUT));
	}
}
