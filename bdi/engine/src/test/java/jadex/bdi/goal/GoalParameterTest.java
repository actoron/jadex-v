package jadex.bdi.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalParameter;
import jadex.bdi.goal.GoalParameterTest.GoalParameterTestAgent.MyGoal;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.injection.AbstractDynVal.ObservationMode;
import jadex.injection.AbstractDynamicValueTest;
import jadex.injection.Dyn;
import jadex.injection.Val;
import jadex.injection.annotation.OnStart;

/**
 *  Test events from all kinds of beliefs.
 */
public class GoalParameterTest	extends AbstractDynamicValueTest
{
	@BDIAgent
	static class GoalParameterTestAgent extends AbstractDynamicValueTest.AbstractDynamicValueTestAgent
	{
		@Goal
		class MyGoal
		{
			@GoalParameter
			Val<Integer>	val;

			@GoalParameter
			Val<Bean>	valbean;
			
			// Test that nested generic types work
			@GoalParameter
			Val<Supplier<String>>	valsupplier;
			
			// Test that nested generic types work
			@GoalParameter
			Val<List<Supplier<String>>>	vallistsupplier;
			
			@GoalParameter
			Bean	bean;
			
			@GoalParameter
			List<String>	list;

			@GoalParameter
			Val<List<String>>	vallist;

			@GoalParameter
			Val<List<Bean>>	vallistbean;
			
			@GoalParameter
			List<Bean>	listbean;

			@GoalParameter
			Set<String>	set;
			
			@GoalParameter
			Map<String, String>	map;

			@GoalParameter
			Dyn<Integer>	dynamic	= new Dyn<>(new Callable<Integer>()
			{
				@Override
				public Integer call() throws Exception
				{
					return getVal().get()+1;
				}
			});
			
			@GoalParameter
			public Dyn<Long>	update	= new Dyn<>(()->System.currentTimeMillis())
				.setUpdateRate(1000)
				// Test if byte code can be analyzed when also calling setObservationMode
				.setObservationMode(ObservationMode.ON_ALL_CHANGES);

		}
		
		// Goal singleton used to access the parameters.
		MyGoal	mygoal;
		MyGoal getMyGoal()
		{
			return mygoal!=null ? mygoal : (mygoal=new MyGoal());
		}
		
		@OnStart
		void init(IBDIAgentFeature bdi)
		{
			bdi.dispatchTopLevelGoal(mygoal);
		}
		
		@Override
		public Val<Integer> getVal() { return getMyGoal().val; }
		@Override
		public void initVal(Val<Integer> ini) { getMyGoal().val=ini; }
		
		@Override
		public Val<Bean> getValBean() { return getMyGoal().valbean; }
		@Override
		public void initValBean(Val<Bean> ini) { getMyGoal().valbean=ini; }
		
		@Override
		public Val<Supplier<String>> getValSupplier() { return getMyGoal().valsupplier; }
		
		@Override
		public Val<List<Supplier<String>>> getValListSupplier() { return getMyGoal().vallistsupplier; }
		
		@Override
		public Bean getBean() { return getMyGoal().bean; }
		@Override
		public void initBean(Bean ini) { getMyGoal().bean=ini; }
		
		@Override
		public List<String> getList() { return getMyGoal().list; }
		@Override
		public void initList(List<String> ini) { getMyGoal().list=ini; }
		
		@Override
		public Val<List<String>> getValList() { return getMyGoal().vallist; }
		@Override
		public void initValList(Val<List<String>> ini) { getMyGoal().vallist=ini; }
		
		@Override
		public Val<List<Bean>> getValListBean() { return getMyGoal().vallistbean; }
		@Override
		public void initValListBean(Val<List<Bean>> ini) { getMyGoal().vallistbean=ini; }
		
		@Override
		public List<Bean> getListBean() { return getMyGoal().listbean; }
		@Override
		public void initListBean(List<Bean> ini) { getMyGoal().listbean=ini; }
		
		@Override
		public Set<String> getSet() { return getMyGoal().set; }
		@Override
		public void initSet(Set<String> ini) { getMyGoal().set=ini; }

		@Override
		public Map<String, String> getMap() { return getMyGoal().map; }
		@Override
		public void initMap(Map<String, String> ini) { getMyGoal().map=ini; }
		
		@Override
		public Dyn<Integer> getDynamic() { return getMyGoal().dynamic; }

		@Override
		public Dyn<Long> getUpdate() { return getMyGoal().update; }
	}
	
	@Override
	public void testDynamic()
	{
		// TODO: generic dependency mechanism
	}
	
	//-------- helper methods --------
	
	@Override
	public AbstractDynamicValueTestAgent createAgent()
	{
		return new GoalParameterTestAgent();
	}

	static int	rulecnt	= 0;
	
	@Override
	public void	addEventListener(Future<Object> fut, Type type, String name)
	{
		name	= MyGoal.class.getName()+"."+name;
		IBDIAgentFeature	feat	= IComponentManager.get().getCurrentComponent()
			.getFeature(IBDIAgentFeature.class);
		feat.addChangeListener(name, event ->
		{
			if(type==event.type())
			{
				fut.setResultIfUndone(event);
			}
		});
	}
	
	@Override
	public void	addEventListener(IntermediateFuture<Object> fut, Type type, String name)
	{
		name	= MyGoal.class.getName()+"."+name;
		IBDIAgentFeature	feat	= IComponentManager.get().getCurrentComponent()
			.getFeature(IBDIAgentFeature.class);
		feat.addChangeListener(name, event ->
		{
			if(type==event.type())
			{
				fut.addIntermediateResultIfUndone(event);
			}
		});
	}
	
	@Override
	public void checkEventInfo(IFuture<Object> fut, Object oldval, Object newval, Object info)
	{
		ChangeEvent	event	= (ChangeEvent) fut.get(TestHelper.TIMEOUT);
		assertEquals(oldval, event.oldvalue(), "old value");
		assertEquals(newval, event.value(), "new value");
		assertEquals(info, event.info(), "info");
	}
	
	@Override
	public void checkEventInfo(IIntermediateFuture<Object> fut, Object oldval, Object newval, Object info)
	{
		ChangeEvent	event	= (ChangeEvent) fut.getNextIntermediateResult(TestHelper.TIMEOUT);
		assertEquals(oldval, event.oldvalue(), "old value");
		assertEquals(newval, event.value(), "new value");
		assertEquals(info, event.info(), "info");
	}
}
