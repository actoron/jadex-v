package jadex.injection;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.injection.AbstractDynVal.ObservationMode;
import jadex.injection.annotation.DynamicValue;

/**
 *  Test events from all kinds of dynamic values.
 */
public class DynamicValuesTest	extends AbstractDynamicValueTest
{
	static class DynamicValueTestAgent extends AbstractDynamicValueTest.AbstractDynamicValueTestAgent
	{
		@DynamicValue
		Val<Integer>	val;
		@Override
		public Val<Integer> getVal() { return val; }
		@Override
		public void initVal(Val<Integer> ini) { val=ini; }
		
		@DynamicValue
		Val<Bean>	valbean;
		@Override
		public Val<Bean> getValBean() { return valbean; }
		@Override
		public void initValBean(Val<Bean> ini) { valbean=ini; }
		
		// Test that nested generic types work
		@DynamicValue
		Val<Supplier<String>>	valsupplier;
		@Override
		public Val<Supplier<String>> getValSupplier() { return valsupplier; }
		
		// Test that nested generic types work
		@DynamicValue
		Val<List<Supplier<String>>>	vallistsupplier;
		@Override
		public Val<List<Supplier<String>>> getValListSupplier() { return vallistsupplier; }
		
		@DynamicValue
		Bean	bean;
		@Override
		public Bean getBean() { return bean; }
		@Override
		public void initBean(Bean ini) { bean=ini; }
		
		@DynamicValue
		List<String>	list;
		@Override
		public List<String> getList() { return list; }
		@Override
		public void initList(List<String> ini) { list=ini; }
		
		@DynamicValue
		Val<List<String>>	vallist;
		@Override
		public Val<List<String>> getValList() { return vallist; }
		@Override
		public void initValList(Val<List<String>> ini) { vallist=ini; }
		
		@DynamicValue
		Val<List<Bean>>	vallistbean;
		@Override
		public Val<List<Bean>> getValListBean() { return vallistbean; }
		@Override
		public void initValListBean(Val<List<Bean>> ini) { vallistbean=ini; }
		
		@DynamicValue
		List<Bean>	listbean;
		@Override
		public List<Bean> getListBean() { return listbean; }
		@Override
		public void initListBean(List<Bean> ini) { listbean=ini; }
		
		@DynamicValue
		Set<String>	set;
		@Override
		public Set<String> getSet() { return set; }
		@Override
		public void initSet(Set<String> ini) { set=ini; }

		@DynamicValue
		Map<String, String>	map;
		@Override
		public Map<String, String> getMap() { return map; }
		@Override
		public void initMap(Map<String, String> ini) { map=ini; }
		
		@DynamicValue
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

		@DynamicValue
		public Dyn<Long>	update	= new Dyn<>(()->System.currentTimeMillis())
			.setUpdateRate(1000)
			// Test if byte code can be analyzed when also calling setObservationMode
			.setObservationMode(ObservationMode.ON_ALL_CHANGES);
		@Override
		public Dyn<Long> getUpdate() { return update; }
	}
	
	//-------- helper methods --------
	
	@Override
	public AbstractDynamicValueTestAgent createAgent()
	{
		return new DynamicValueTestAgent();
	}
	
	@Override
	public void	addEventListener(Future<Object> fut, Type type, String name)
	{
		IComponentManager.get().getCurrentComponent()
			.getFeature(IInjectionFeature.class).addListener(name, event ->
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
		IComponentManager.get().getCurrentComponent()
			.getFeature(IInjectionFeature.class).addListener(name, event ->
		{
			if(type==event.type())
			{
				fut.addIntermediateResult(event);
			}
		});
	}
	
	@Override
	public void checkEventInfo(IFuture<Object> fut, Object oldval, Object newval, Object info)
	{
		ChangeEvent	event	= (ChangeEvent) fut.get(TIMEOUT);
		assertEquals(oldval, event.oldvalue(), "old value");
		assertEquals(newval, event.value(), "new value");
		assertEquals(info, event.info(), "info");			
	}
	
	@Override
	public void checkEventInfo(IIntermediateFuture<Object> fut, Object oldval, Object newval, Object info)
	{
		ChangeEvent	event	= (ChangeEvent) fut.getNextIntermediateResult(TIMEOUT);
		assertEquals(oldval, event.oldvalue(), "old value");
		assertEquals(newval, event.value(), "new value");
		assertEquals(info, event.info(), "info");			
	}
}
