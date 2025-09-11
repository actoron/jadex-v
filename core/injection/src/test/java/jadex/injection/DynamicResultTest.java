package jadex.injection;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.common.NameValue;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.injection.AbstractDynVal.ObservationMode;
import jadex.injection.annotation.ProvideResult;

/**
 *  Test events from all kinds of dynamic results.
 */
public class DynamicResultTest	//extends AbstractDynamicValueTest
{
//	static class DynamicResultTestAgent extends AbstractDynamicValueTest.AbstractDynamicValueTestAgent
//	{
//		@ProvideResult
//		Val<Integer>	val;
//		@Override
//		public Val<Integer> getVal() { return val; }
//		@Override
//		public void initVal(Val<Integer> ini) { val=ini; }
//		
//		@ProvideResult
//		Val<Bean>	valbean;
//		@Override
//		public Val<Bean> getValBean() { return valbean; }
//		@Override
//		public void initValBean(Val<Bean> ini) { valbean=ini; }
//		
//		// Test that nested generic types work
//		@ProvideResult
//		Val<Supplier<String>>	valsupplier;
//		@Override
//		public Val<Supplier<String>> getValSupplier() { return valsupplier; }
//		
//		// Test that nested generic types work
//		@ProvideResult
//		Val<List<Supplier<String>>>	vallistsupplier;
//		@Override
//		public Val<List<Supplier<String>>> getValListSupplier() { return vallistsupplier; }
//		
//		@ProvideResult
//		Bean	bean;
//		@Override
//		public Bean getBean() { return bean; }
//		@Override
//		public void initBean(Bean ini) { bean=ini; }
//		
//		@ProvideResult
//		List<String>	list;
//		@Override
//		public List<String> getList() { return list; }
//		@Override
//		public void initList(List<String> ini) { list=ini; }
//		
//		@ProvideResult
//		Val<List<String>>	vallist;
//		@Override
//		public Val<List<String>> getValList() { return vallist; }
//		@Override
//		public void initValList(Val<List<String>> ini) { vallist=ini; }
//		
//		@ProvideResult
//		Val<List<Bean>>	vallistbean;
//		@Override
//		public Val<List<Bean>> getValListBean() { return vallistbean; }
//		@Override
//		public void initValListBean(Val<List<Bean>> ini) { vallistbean=ini; }
//		
//		@ProvideResult
//		List<Bean>	listbean;
//		@Override
//		public List<Bean> getListBean() { return listbean; }
//		@Override
//		public void initListBean(List<Bean> ini) { listbean=ini; }
//		
//		@ProvideResult
//		Set<String>	set;
//		@Override
//		public Set<String> getSet() { return set; }
//		@Override
//		public void initSet(Set<String> ini) { set=ini; }
//
//		@ProvideResult
//		Map<String, String>	map;
//		@Override
//		public Map<String, String> getMap() { return map; }
//		@Override
//		public void initMap(Map<String, String> ini) { map=ini; }
//		
//		@ProvideResult
//		Dyn<Integer>	dynamic	= new Dyn<>(new Callable<Integer>()
//		{
//			@Override
//			public Integer call() throws Exception
//			{
//				return getVal().get()+1;
//			}
//		});
//		@Override
//		public Dyn<Integer> getDynamic() { return dynamic; }
//
//		@ProvideResult
//		public Dyn<Long>	update	= new Dyn<>(()->System.currentTimeMillis())
//			.setUpdateRate(1000)
//			// Test if byte code can be analyzed when also calling setObservationMode
//			.setObservationMode(ObservationMode.ON_ALL_CHANGES);
//		@Override
//		public Dyn<Long> getUpdate() { return update; }
//	}
//	
//	//-------- helper methods --------
//	
//	@Override
//	public AbstractDynamicValueTestAgent createAgent()
//	{
//		return new DynamicResultTestAgent();
//	}
//	
//	Map<Future<Object>, ChangeType>	changetypes	= new LinkedHashMap<>();
//	
//	@Override
//	public void	addEventListener(Future<Object> fut, ChangeType type, String name)
//	{
//		if(type==ChangeType.ADDED || type==ChangeType.CHANGED)
//		{
//			// Remember type for intermediate future check.
//			changetypes.put(fut, type);
//			
//			// Subscribe to results and filter for the correct name.
//			ISubscriptionIntermediateFuture<NameValue>	sub	= 
//				IComponentManager.get().getCurrentComponent()
//				.getComponentHandle().subscribeToResults();
//			sub.next(res ->
//			{
//				if(name.equals(res.name()))
//				{
//					fut.setResultIfUndone(res.value());
//				}
//			});
//		}
//	}
//	
//	@Override
//	public void	addEventListener(IntermediateFuture<Object> fut, ChangeType type, String name)
//	{
//		if(type==ChangeType.ADDED || type==ChangeType.CHANGED)
//		{
//			// Remember type for intermediate future check.
//			@SuppressWarnings("rawtypes")
//			Future	fut0 = fut;	
//			@SuppressWarnings("unchecked")
//			Future<Object>	fut00 = fut0;
//			changetypes.put(fut00, type);
//			
//			// Subscribe to results and filter for the correct name.
//			ISubscriptionIntermediateFuture<NameValue>	sub	= 
//				IComponentManager.get().getCurrentComponent()
//				.getComponentHandle().subscribeToResults();
//			sub.next(res ->
//			{
//				if(name.equals(res.name()))
//				{
//					fut.addIntermediateResult(res.value());
//				}
//			});
//		}
//	}
//	
//	@Override
//	public void checkEventInfo(IFuture<Object> fut, Object oldval, Object newval, Object info)
//	{
//		ChangeType	type	= changetypes.get(fut);
//		if(type==ChangeType.ADDED || type==ChangeType.CHANGED)
//		{
//			Object	result	= fut.get(TIMEOUT);
//			System.out.println("Result: "+result+", expected: "+newval+", old: "+oldval+", info: "+info);
//			
////			assertEquals(oldval, ci.getOldValue(), "old value");
////			assertEquals(newval, ci.getValue(), "new value");
////			assertEquals(info, ci.getInfo(), "info");			
//		}
//	}
//	
//	@Override
//	public void checkEventInfo(IIntermediateFuture<Object> fut, Object oldval, Object newval, Object info)
//	{
////		IEvent	event	= (IEvent) fut.getNextIntermediateResult(TestHelper.TIMEOUT);
////		@SuppressWarnings("unchecked")
////		ChangeInfo<Object>	ci	= (ChangeInfo<Object>)event.getContent();
////		assertEquals(oldval, ci.getOldValue(), "old value");
////		assertEquals(newval, ci.getValue(), "new value");
////		assertEquals(info, ci.getInfo(), "info");
//	}
}
