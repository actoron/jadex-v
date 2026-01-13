package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jadex.common.TimeoutException;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.ChangeEvent.Type;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.injection.AbstractDynVal.ObservationMode;

/**
 *  Test events from all kinds of beliefs.
 */
public abstract class AbstractDynamicValueTest
{
	long	TIMEOUT	= 10000;
	
	public static abstract class AbstractDynamicValueTestAgent
	{
		public AbstractDynamicValueTestAgent()
		{
			initVal(new Val<>(1));
			initValBean(new Val<>(new Bean(0)));
			initBean(new Bean(1));
			initList(new ArrayList<>(Arrays.asList(new String[]{"1", "2"})));
			initValList(new Val<>(new ArrayList<>(Arrays.asList(new String[]{"1", "2"}))));
			initValListBean(new Val<>(new ArrayList<>(Arrays.asList(new Bean(1), new Bean(2)))));
			initListBean(new ArrayList<>(Arrays.asList(new Bean(1), new Bean(2))));
			initSet(new LinkedHashSet<>(Arrays.asList(new String[]{"1", "3"})));
			
			Map<String, String>	inimap	= new LinkedHashMap<>();
			inimap.put("1", "one");
			inimap.put("2", "wto");
			initMap(inimap);
		}
		
		public abstract Val<Integer>	getVal();
		public abstract void	initVal(Val<Integer> val);

		public abstract Val<Bean>	getValBean();
		public abstract void	initValBean(Val<Bean> val);

		// Reminders to test that nested generic types can be parsed
		public abstract Val<Supplier<String>>	getValSupplier();
		public abstract Val<List<Supplier<String>>> getValListSupplier();
		
		public abstract Bean	getBean();
		public abstract void	initBean(Bean bean);
		
		public abstract List<String>	getList();
		public abstract void	initList(List<String> list);
		
		public abstract Val<List<String>>	getValList();
		public abstract void	initValList(Val<List<String>> list);
		
		public abstract Val<List<Bean>>	getValListBean();
		public abstract void	initValListBean(Val<List<Bean>> list);
		
		public abstract List<Bean>	getListBean();
		public abstract void	initListBean(List<Bean> list);
		
		public abstract Set<String>	getSet();
		public abstract void	initSet(Set<String> set);

		public abstract Map<String, String>	getMap();
		public abstract void	initMap(Map<String, String> map);
		
		public abstract Dyn<Integer>	getDynamic();
		// Test if byte code can be analyzed when using new Callable...
//		Dyn<Integer>	dynamicbelief	= new Dyn<>(new Callable<Integer>()
//		{
//			@Override
//			public Integer call() throws Exception
//			{
//				return val().get()+1;
//			}
//		});

		public abstract Dyn<Long>	getUpdate(); 
		// Test if byte code can be analyzed when using lambda
//		Dyn<Long>	updatebelief	= new Dyn<>(()->System.currentTimeMillis())
//			// Test if byte code can be analyzed when calling setUpdateRate
//			.setUpdateRate(1000)
//			// Test if byte code can be analyzed when calling setObservationMode
//			.setObservationMode(ObservationMode.ON_ALL_CHANGES);
	}
	
	public static class Bean
	{
		int value;
		
		public Bean(int value)
		{
			this.value	= value;
		}
		
		PropertyChangeSupport	pcs	= new PropertyChangeSupport(this);
		public void	addPropertyChangeListener(PropertyChangeListener pcl)
		{
			pcs.addPropertyChangeListener(pcl);
		}
		public void	removePropertyChangeListener(PropertyChangeListener pcl)
		{
			pcs.removePropertyChangeListener(pcl);
		}
		
		public void setValue(int value)
		{
			int	oldvalue	= this.value;
			this.value = value;
			pcs.firePropertyChange("value", oldvalue, value);
		}
		
		public String toString()
		{
			return Integer.toString(value);
		}
		
		public int hashCode()
		{
			return 31+value;
		}
		
		public boolean equals(Object o)
		{
			return o instanceof Bean && ((Bean)o).value==value;
		}
	}
	
	@Test
	public void testVal()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		Future<Object>	fut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListener(fut, Type.CHANGED, "val");
			pojo.getVal().set(2);
		}).get(TIMEOUT);
		
		checkEventInfo(fut, 1, 2, null);
	}

	@Test
	public void testBean()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		Future<Object>	fut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListener(fut, Type.CHANGED, "bean");
			pojo.getBean().setValue(2);
		}).get(TIMEOUT);
		
		checkEventInfo(fut, null, pojo.getBean(), "value");
	}

	@Test
	public void testValBean()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		
		// Test immediate set of bean before init, i.e. observe initial bean.
		Future<Object>	fut1	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(fut1, Type.CHANGED, "valbean");
			pojo.getValBean().get().setValue(1);
			return null;
		}).get(TIMEOUT);
		checkEventInfo(fut1, null, pojo.getValBean().get(), "value");
		
		// Test delayed set of bean after init.
		Future<Object>	fut2	= new Future<>();
		Bean	old = pojo.getValBean().get();
		exta.scheduleStep(() ->
		{
			addEventListener(fut2, Type.CHANGED, "valbean");
			pojo.getValBean().set(new Bean(2));
			return null;
		}).get(TIMEOUT);
		checkEventInfo(fut2, old, pojo.getValBean().get(), null);
		
		// Test set property of changed bean.
		Future<Object>	fut3	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(fut3, Type.CHANGED, "valbean");
			pojo.getValBean().get().setValue(3);
			return null;
		}).get(TIMEOUT);
		checkEventInfo(fut3, null, pojo.getValBean().get(), "value");
		
		// Test observation mode ON_SET_VALUE only.
		exta.scheduleStep(() ->
		{
			pojo.getValBean().setObservationMode(ObservationMode.ON_SET_VALUE);
			return null;
		}).get(TIMEOUT);
		
		// Test set of val to new bean (should throw event).
		Future<Object>	fut4	= new Future<>();
		Bean	old2 = pojo.getValBean().get();
		exta.scheduleStep(() ->
		{
			addEventListener(fut4, Type.CHANGED, "valbean");
			pojo.getValBean().set(new Bean(4));
			return null;
		}).get(TIMEOUT);
		checkEventInfo(fut4, old2, pojo.getValBean().get(), null);
		
		// Test set of bean property (should not throw event).
		Future<Object>	fut5	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(fut5, Type.CHANGED, "valbean");
			pojo.getValBean().get().setValue(5);
			return null;
		});
		assertThrows(TimeoutException.class, () -> fut5.get(500)); // No event should be generated
		
		// Test observation mode OFF.
		exta.scheduleStep(() ->
		{
			pojo.getValBean().setObservationMode(ObservationMode.OFF);
			return null;
		}).get(TIMEOUT);
		
		// Test set of val to new bean (should not throw event).
		Future<Object>	fut6	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(fut6, Type.CHANGED, "valbean");
			pojo.getValBean().set(new Bean(6));
			return null;
		}).get(TIMEOUT);
		assertThrows(TimeoutException.class, () -> fut6.get(500)); // No event should be generated
		
		// Test setting observation mode back to BEAN.
		exta.scheduleStep(() ->
		{
			pojo.getValBean().setObservationMode(ObservationMode.ON_BEAN_CHANGE);
			return null;
		}).get(TIMEOUT);
		
		// Test set property changed bean.
		Future<Object>	fut7	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(fut7, Type.CHANGED, "valbean");
			pojo.getValBean().get().setValue(7);
			return null;
		}).get(TIMEOUT);
		checkEventInfo(fut7, null, pojo.getValBean().get(), "value");

	}

	@Test
	public void testList()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		Future<Object>	changedfut	= new Future<>();
		Future<Object>	addedfut	= new Future<>();
		Future<Object>	removedfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut, Type.CHANGED, "list");
			addEventListener(addedfut, Type.ADDED, "list");
			addEventListener(removedfut, Type.REMOVED, "list");
			pojo.getList().set(1, "3");
			pojo.getList().add(1, "2");
			pojo.getList().remove(1);

		}).get(TIMEOUT);
		
		checkEventInfo(changedfut, "2", "3", 1);
		checkEventInfo(addedfut, null, "2", 1);
		checkEventInfo(removedfut, null, "2", 1);
	}

	@Test
	public void testListBean()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		
		// Check list events
		Future<Object>	changedfut	= new Future<>();
		Future<Object>	addedfut	= new Future<>();
		Future<Object>	removedfut	= new Future<>();
		Bean[] removedbean	= new Bean[1];
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut, Type.CHANGED, "listbean");
			addEventListener(addedfut, Type.ADDED, "listbean");
			addEventListener(removedfut, Type.REMOVED, "listbean");
			pojo.getListBean().set(1, new Bean(3));
			pojo.getListBean().add(1, new Bean(2));
			removedbean[0]	= pojo.getListBean().remove(1);
			return null;
		}).get(TIMEOUT);
		checkEventInfo(changedfut, new Bean(2), new Bean(3), 1);
		checkEventInfo(addedfut, null, new Bean(2), 1);
		checkEventInfo(removedfut, null, new Bean(2), 1);
		
		// Check no event on change of removed bean
		Future<Object>	changedfut2	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut2, Type.CHANGED, "listbean");
			removedbean[0].setValue(4);
			return null;
		}).get(TIMEOUT);
		assertThrows(TimeoutException.class, () -> changedfut2.get(100)); // No event should be generated
		
		// Check bean property change events
		Future<Object>	changedfut3	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut3, Type.CHANGED, "listbean");
			pojo.getListBean().get(1).setValue(4);
		}).get(TIMEOUT);
		checkEventInfo(changedfut3, null, Arrays.asList(new Bean(1), new Bean(4)), null);
	}

	@Test
	public void testValListBean()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		
		// Check list events
		Future<Object>	changedfut	= new Future<>();
		Future<Object>	addedfut	= new Future<>();
		Future<Object>	removedfut	= new Future<>();
		Bean[] removedbean	= new Bean[1];
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut, Type.CHANGED, "vallistbean");
			addEventListener(addedfut, Type.ADDED, "vallistbean");
			addEventListener(removedfut, Type.REMOVED, "vallistbean");
			pojo.getValListBean().get().set(1, new Bean(3));
			pojo.getValListBean().get().add(1, new Bean(2));
			removedbean[0]	= pojo.getValListBean().get().remove(1);
			return null;
		}).get(TIMEOUT);
		checkEventInfo(changedfut, new Bean(2), new Bean(3), 1);
		checkEventInfo(addedfut, null, new Bean(2), 1);
		checkEventInfo(removedfut, null, new Bean(2), 1);
		
		// Check no event on change of removed bean
		Future<Object>	changedfut2	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut2, Type.CHANGED, "vallistbean");
			removedbean[0].setValue(4);
			return null;
		}).get(TIMEOUT);
		assertThrows(TimeoutException.class, () -> changedfut2.get(100)); // No event should be generated
		
		// Check bean property change events
		Future<Object>	changedfut3	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut3, Type.CHANGED, "vallistbean");
			pojo.getValListBean().get().get(1).setValue(4);
		}).get(TIMEOUT);
		checkEventInfo(changedfut3, null, Arrays.asList(new Bean(1), new Bean(4)), null);
		
		// Check turning off bean property change events
		Future<Object>	changedfut4	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut4, Type.CHANGED, "vallistbean");
			pojo.getValListBean().setObservationMode(ObservationMode.ON_COLLECTION_CHANGE);
			pojo.getValListBean().get().get(1).setValue(5);
			return null;
		}).get(TIMEOUT);
		assertThrows(TimeoutException.class, () -> changedfut4.get(100)); // No event should be generated
		
		// Check turning back on bean property change events
		Future<Object>	changedfut5	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut5, Type.CHANGED, "vallistbean");
			pojo.getValListBean().setObservationMode(ObservationMode.ON_ALL_CHANGES);
			pojo.getValListBean().get().get(1).setValue(6);
		}).get(TIMEOUT);
		checkEventInfo(changedfut5, null, Arrays.asList(new Bean(1), new Bean(6)), null);
	}

	@Test
	public void testValList()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		IntermediateFuture<Object>	changedfut	= new IntermediateFuture<>();
		IntermediateFuture<Object>	addedfut	= new IntermediateFuture<>();
		IntermediateFuture<Object>	removedfut	= new IntermediateFuture<>();
		
		// Check if events are generated.
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut, Type.CHANGED, "vallist");
			addEventListener(addedfut, Type.ADDED, "vallist");
			addEventListener(removedfut, Type.REMOVED, "vallist");
			pojo.getValList().get().set(1, "3");
			pojo.getValList().get().add(1, "2");
			pojo.getValList().get().remove(1);

		}).get(TIMEOUT);
		checkEventInfo(changedfut, "2", "3", 1);
		checkEventInfo(addedfut, null, "2", 1);
		checkEventInfo(removedfut, null, "2", 1);
		
		// Check if no events are generated after setting observation mode to OFF.
		exta.scheduleStep(() ->
		{
			pojo.getValList().setObservationMode(ObservationMode.OFF);
			pojo.getValList().get().add(1, "2");
			pojo.getValList().get().set(1, "3");
			pojo.getValList().get().remove(1);
			return null;
		}).get(TIMEOUT);
		assertFalse(changedfut.hasNextIntermediateResult(100, true));
		assertFalse(addedfut.hasNextIntermediateResult(100, true));
		assertFalse(removedfut.hasNextIntermediateResult(100, true));
		
		// Check if events are generated after resetting observation mode.
		exta.scheduleStep(() ->
		{
			pojo.getValList().setObservationMode(ObservationMode.ON_COLLECTION_CHANGE);
			pojo.getValList().get().add(1, "2");
			pojo.getValList().get().set(1, "3");
			pojo.getValList().get().remove(1);
			return null;
		}).get(TIMEOUT);
		checkEventInfo(changedfut, "2", "3", 1);
		checkEventInfo(addedfut, null, "2", 1);
		checkEventInfo(removedfut, null, "3", 1);
	}

	@Test
	public void testSet()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		Future<Object>	addedfut	= new Future<>();
		Future<Object>	removedfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListener(addedfut, Type.ADDED, "set");
			addEventListener(removedfut, Type.REMOVED, "set");
			pojo.getSet().add("2");
			pojo.getSet().remove("2");

		}).get(TIMEOUT);
		
		checkEventInfo(addedfut, null, "2", null);
		checkEventInfo(removedfut, null, "2", null);
	}
	
	@Test
	public void testMap()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		Future<Object>	changedfut	= new Future<>();
		Future<Object>	addedfut	= new Future<>();
		Future<Object>	removedfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut, Type.CHANGED, "map");
			addEventListener(addedfut, Type.ADDED, "map");
			addEventListener(removedfut, Type.REMOVED, "map");
			pojo.getMap().put("2", "two");
			pojo.getMap().put("3", "three");
			pojo.getMap().remove("2");

		}).get(TIMEOUT);
		
		checkEventInfo(changedfut, "wto", "two", "2");
		checkEventInfo(addedfut, null, "three", "3");
		checkEventInfo(removedfut, null, "two", "2");
	}

	@Test
	public void testDynamic()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		Future<Integer>	firstfut	= new Future<>();
		Future<Object>	changedfut	= new Future<>();
		Future<Integer>	secondfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut, Type.CHANGED, "dynamic");
			firstfut.setResult(pojo.getDynamic().get());
			pojo.getVal().set(2);
			secondfut.setResult(pojo.getDynamic().get());
		}).get(TIMEOUT);
		
		assertEquals(2, firstfut.get(TIMEOUT));
		assertEquals(3, secondfut.get(TIMEOUT));
//		assertThrows(IllegalStateException.class, ()->exfut.get(TIMEOUT));
		checkEventInfo(changedfut, 2, 3, null);
	}
	
	@Test
	public void testUpdaterate()
	{
		AbstractDynamicValueTestAgent	pojo	= createAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TIMEOUT);
		Future<Long>	firstfut	= new Future<>();
		Future<Long>	secondfut	= new Future<>();
		Future<Object>	changedfut	= new Future<>();
		Future<Long>	thirdfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListener(changedfut, Type.CHANGED, "update");
			firstfut.setResult(pojo.getUpdate().get());
			IExecutionFeature.get().waitForDelay(500).get();
			secondfut.setResult(pojo.getUpdate().get());
			IExecutionFeature.get().waitForDelay(1000).get();
			thirdfut.setResult(pojo.getUpdate().get());
		}).get(TIMEOUT);
		
		assertNotNull(firstfut.get(TIMEOUT));
		assertEquals(firstfut.get(TIMEOUT), secondfut.get(TIMEOUT));
		assertNotEquals(firstfut.get(TIMEOUT), thirdfut.get(TIMEOUT));
		changedfut.get(TIMEOUT);	// Check if event was generated
		
		// Test changing update rate while running.
		Future<Long>	fourthfut	= new Future<>();
		Future<Long>	fifthfut	= new Future<>();
		exta.scheduleStep(() ->
		{
			pojo.getUpdate().setUpdateRate(5000);
			fourthfut.setResult(pojo.getUpdate().get());
			IExecutionFeature.get().waitForDelay(1500).get();
			fifthfut.setResult(pojo.getUpdate().get());
		}).get(TIMEOUT);
		assertEquals(fourthfut.get(TIMEOUT), fifthfut.get(TIMEOUT));
	}
	
	//-------- helper methods --------
	
	/**
	 *  Create the test agent.
	 */
	public abstract AbstractDynamicValueTestAgent createAgent();
	
	/**
	 *  Add a handler that sets the event into a future.
	 *  Called on agent thread.
	 */
	public abstract void	addEventListener(Future<Object> fut, Type type, String name);
	
	/**
	 *  Add a handler that adds the event into a future.
	 *  Called on agent thread.
	 */
	public abstract void	addEventListener(IntermediateFuture<Object> fut, Type type, String name);
		
	/**
	 *  Check if old/new value and info match expectations.
	 */
	public abstract void checkEventInfo(IFuture<Object> fut, Object oldval, Object newval, Object info);
	
	/**
	 *  Check if old/new value and info match expectations.
	 */
	public abstract void checkEventInfo(IIntermediateFuture<Object> fut, Object oldval, Object newval, Object info);
}
