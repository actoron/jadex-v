package jadex.bdi.belief;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IBeliefListener;
import jadex.bdi.TestHelper;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.ChangeEvent;
import jadex.common.Tuple2;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.injection.AbstractDynVal.ObservationMode;
import jadex.injection.AbstractDynamicValueTest;
import jadex.injection.Dyn;
import jadex.injection.Val;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.Rule;

/**
 *  Test events from all kinds of beliefs.
 */
public class BeliefTest	extends AbstractDynamicValueTest
{
	@BDIAgent
	static class BeliefTestAgent extends AbstractDynamicValueTest.AbstractDynamicValueTestAgent
	{
		@Belief
		Val<Integer>	val;
		@Override
		public Val<Integer> getVal() { return val; }
		@Override
		public void initVal(Val<Integer> ini) { val=ini; }
		
		@Belief
		Val<Bean>	valbean;
		@Override
		public Val<Bean> getValBean() { return valbean; }
		@Override
		public void initValBean(Val<Bean> ini) { valbean=ini; }
		
		// Test that nested generic types work
		@Belief
		Val<Supplier<String>>	valsupplier;
		@Override
		public Val<Supplier<String>> getValSupplier() { return valsupplier; }
		
		// Test that nested generic types work
		@Belief
		Val<List<Supplier<String>>>	vallistsupplier;
		@Override
		public Val<List<Supplier<String>>> getValListSupplier() { return vallistsupplier; }
		
		@Belief
		Bean	bean;
		@Override
		public Bean getBean() { return bean; }
		@Override
		public void initBean(Bean ini) { bean=ini; }
		
		@Belief
		List<String>	list;
		@Override
		public List<String> getList() { return list; }
		@Override
		public void initList(List<String> ini) { list=ini; }
		
		@Belief
		Val<List<String>>	vallist;
		@Override
		public Val<List<String>> getValList() { return vallist; }
		@Override
		public void initValList(Val<List<String>> ini) { vallist=ini; }
		
		@Belief
		Val<List<Bean>>	vallistbean;
		@Override
		public Val<List<Bean>> getValListBean() { return vallistbean; }
		@Override
		public void initValListBean(Val<List<Bean>> ini) { vallistbean=ini; }
		
		@Belief
		List<Bean>	listbean;
		@Override
		public List<Bean> getListBean() { return listbean; }
		@Override
		public void initListBean(List<Bean> ini) { listbean=ini; }
		
		@Belief
		Set<String>	set;
		@Override
		public Set<String> getSet() { return set; }
		@Override
		public void initSet(Set<String> ini) { set=ini; }

		@Belief
		Map<String, String>	map;
		@Override
		public Map<String, String> getMap() { return map; }
		@Override
		public void initMap(Map<String, String> ini) { map=ini; }
		
		@Belief
		Dyn<Integer>	dynamic	= new Dyn<>(new Callable<Integer>()
		{
			@Override
			public Integer call() throws Exception
			{
				return getVal().get()+1;
			}
		});
		@Override
		public Dyn<Integer> getDynamic() { return dynamic; }

		@Belief
		public Dyn<Long>	update	= new Dyn<>(()->System.currentTimeMillis())
			.setUpdateRate(1000)
			// Test if byte code can be analyzed when also calling setObservationMode
			.setObservationMode(ObservationMode.ON_ALL_CHANGES);
		@Override
		public Dyn<Long> getUpdate() { return update; }
	}
	
	@Test
	public void	testBeliefListener()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		
		// Test fact changed
		List<String>	facts	= new ArrayList<>();
		exta.scheduleStep(comp -> {
			comp.getFeature(IBDIAgentFeature.class)
				.addBeliefListener("val", new IBeliefListener<Integer>()
			{
				@Override
				public void factChanged(ChangeInfo<Integer> info)
				{
					facts.add(info.getOldValue()+":"+info.getValue());
				}
			});
			
			pojo.getVal().set(1);
			pojo.getVal().set(2);
			pojo.getVal().set(3);
			
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("1:2", "2:3"), facts);

		// Test fact added/removed
		List<String>	changes	= new ArrayList<>();
		exta.scheduleStep(comp -> {
			comp.getFeature(IBDIAgentFeature.class)
				.addBeliefListener("set", new IBeliefListener<String>()
			{
				@Override
				public void	factAdded(ChangeInfo<String> info)
				{
					changes.add("a:"+info.getValue());
				}
				
				@Override
				public void	factRemoved(ChangeInfo<String> info)
				{
					changes.add("r:"+info.getValue());
				}
			});
			
			pojo.getSet().add("1");	// already contained
			pojo.getSet().add("2");
			pojo.getSet().remove("1");
			
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("a:2", "r:1"), changes);

		// Test map belief
		List<String>	mchanges	= new ArrayList<>();
		exta.scheduleStep(comp -> {
			comp.getFeature(IBDIAgentFeature.class)
				.addBeliefListener("map", new IBeliefListener<String>()
			{
				@Override
				public void	factChanged(ChangeInfo<String> info)
				{
					mchanges.add("c:"+info.getInfo()+";"+info.getValue());
				}
				
				@Override
				public void	factAdded(ChangeInfo<String> info)
				{
					mchanges.add("a:"+info.getInfo()+";"+info.getValue());
				}
					
				@Override
				public void	factRemoved(ChangeInfo<String> info)
				{
					mchanges.add("r:"+info.getInfo()+";"+info.getValue());
				}
			});
			
			pojo.getMap().put("2", "two");	// already contained
			pojo.getMap().put("3", "three");
			pojo.getMap().remove("1");
			
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("c:2;two", "a:3;three", "r:1;one"), mchanges);
	}
	
	//-------- helper methods --------
	
	@Override
	public AbstractDynamicValueTestAgent createAgent()
	{
		return new BeliefTestAgent();
	}

	static int	rulecnt	= 0;
	
	@Override
	public void	addEventListener(Future<Object> fut, ChangeType type, String name)
	{
		String	bditype =
			(type==ChangeType.CHANGED ? ChangeEvent.FACTCHANGED :
			(type==ChangeType.ADDED ? ChangeEvent.FACTADDED :
			(type==ChangeType.REMOVED ? ChangeEvent.FACTREMOVED : null)));
		String	events[]	= new String[] {bditype, name};
		
		BDIAgentFeature	feat	= (BDIAgentFeature)IComponentManager.get().getCurrentComponent()
			.getFeature(IBDIAgentFeature.class);
		feat.getRuleSystem().getRulebase().addRule(new Rule<Void>(
			"EventListenerRule"+Arrays.toString(events)+"_"+rulecnt++,	// Rule Name
			event -> new Future<>(new Tuple2<Boolean, Object>(true, null)),	// Condition -> true
			(event, rule, context, condresult) -> {fut.setResultIfUndone(event); return IFuture.DONE;}, // Action -> set future
			new EventType[] {new EventType(events)}	// Trigger Event(s)
		));
	}
	
	@Override
	public void	addEventListener(IntermediateFuture<Object> fut, ChangeType type, String name)
	{
		String	bditype =
				(type==ChangeType.CHANGED ? ChangeEvent.FACTCHANGED :
				(type==ChangeType.ADDED ? ChangeEvent.FACTADDED :
				(type==ChangeType.REMOVED ? ChangeEvent.FACTREMOVED : null)));
		String	events[]	= new String[] {bditype, name};
		
		BDIAgentFeature	feat	= (BDIAgentFeature)IComponentManager.get().getCurrentComponent()
			.getFeature(IBDIAgentFeature.class);
		feat.getRuleSystem().getRulebase().addRule(new Rule<Void>(
			"EventListenerRule"+Arrays.toString(events)+"_"+rulecnt++,	// Rule Name
			event -> new Future<>(new Tuple2<Boolean, Object>(true, null)),	// Condition -> true
			(event, rule, context, condresult) -> {fut.addIntermediateResultIfUndone(event); return IFuture.DONE;}, // Action -> set future
			new EventType[] {new EventType(events)}	// Trigger Event(s)
		));
	}
	
	@Override
	public void checkEventInfo(IFuture<Object> fut, Object oldval, Object newval, Object info)
	{
		IEvent	event	= (IEvent) fut.get(TestHelper.TIMEOUT);
		@SuppressWarnings("unchecked")
		ChangeInfo<Object>	ci	= (ChangeInfo<Object>)event.getContent();
		assertEquals(oldval, ci.getOldValue(), "old value");
		assertEquals(newval, ci.getValue(), "new value");
		assertEquals(info, ci.getInfo(), "info");
	}
	
	@Override
	public void checkEventInfo(IIntermediateFuture<Object> fut, Object oldval, Object newval, Object info)
	{
		IEvent	event	= (IEvent) fut.getNextIntermediateResult(TestHelper.TIMEOUT);
		@SuppressWarnings("unchecked")
		ChangeInfo<Object>	ci	= (ChangeInfo<Object>)event.getContent();
		assertEquals(oldval, ci.getOldValue(), "old value");
		assertEquals(newval, ci.getValue(), "new value");
		assertEquals(info, ci.getInfo(), "info");
	}
}
