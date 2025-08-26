package jadex.bdi.belief;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import jadex.bdi.Dyn;
import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IBeliefListener;
import jadex.bdi.TestHelper;
import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.ChangeEvent;
import jadex.common.TimeoutException;
import jadex.common.Tuple2;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.Rule;

/**
 *  Test events from all kinds of beliefs.
 */
public class BeliefTest
{
	@BDIAgent
	static class BeliefTestAgent
	{
		public BeliefTestAgent() {
			this(null);
		}
		
		BeliefTestAgent(Object other)
		{
			
		}
		
		@Belief
		Val<Integer>	valbelief	= new Val<>(1);
		
		@Belief
		Val<Bean>	valbeanbelief	= new Val<>(new Bean(1));
		
		@Belief(observeinner=false)
		Val<Bean>	silentvalbeanbelief	= new Val<>(new Bean(1));
		
		@Belief
		Bean	beanbelief	= new Bean(1);
		
		@Belief
		List<String>	listbelief	= new ArrayList<>(Arrays.asList(new String[]{"1", "2"}));
		
		@Belief
		Set<String>	setbelief	= new LinkedHashSet<>(Arrays.asList(new String[]{"1", "3"}));

		@SuppressWarnings("serial")
		@Belief
		Map<String, String>	mapbelief	= new LinkedHashMap<>()
		{{
			put("1", "one");
			put("2", "wto");
		}};
		
		@Belief
		Dyn<Integer>	dynamicbelief	= new Dyn<>(new Callable<Integer>()
		{
			@Override
			public Integer call() throws Exception
			{
				return valbelief.get()+1;
			}
		});
//		Dyn<Integer>	dynamicbelief	= new Dyn<>(()->valbelief.get()+1);

		@Belief//(updaterate = 1000)
		Dyn<Long>	updatebelief	= new Dyn<>(()->System.currentTimeMillis()).setUpdateRate(1000);
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
	}
	
	@Test
	public void testValBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		Future<IEvent>	fut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(fut, ChangeEvent.FACTCHANGED, "valbelief");
			pojo.valbelief.set(2);
		});
		
		checkEventInfo(fut, 1, 2, null);
	}

	@Test
	public void testBeanBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		Future<IEvent>	fut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(fut, ChangeEvent.FACTCHANGED, "beanbelief");
			pojo.beanbelief.setValue(2);
		});
		
		checkEventInfo(fut, 1, 2, null);
	}

	@Test
	public void testValBeanBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		
		// Test immediate set of bean before init.
		Future<IEvent>	fut1	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListenerRule(fut1, ChangeEvent.FACTCHANGED, "valbeanbelief");
			pojo.valbeanbelief.get().setValue(2);
			return null;
		}).get(TestHelper.TIMEOUT);
		checkEventInfo(fut1, null, pojo.valbeanbelief.get(), null);
		
		// Test delayed set of bean after init.
		Future<IEvent>	fut2	= new Future<>();
		Bean	old = pojo.valbeanbelief.get();
		exta.scheduleStep(() ->
		{
			addEventListenerRule(fut2, ChangeEvent.FACTCHANGED, "valbeanbelief");
			pojo.valbeanbelief.set(new Bean(2));
			return null;
		}).get(TestHelper.TIMEOUT);
		checkEventInfo(fut2, old, pojo.valbeanbelief.get(), null);
		
		// Test delayed set of bean property after init.
		Future<IEvent>	fut3	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListenerRule(fut3, ChangeEvent.FACTCHANGED, "valbeanbelief");
			pojo.valbeanbelief.get().setValue(3);
			return null;
		}).get(TestHelper.TIMEOUT);
		checkEventInfo(fut3, null, pojo.valbeanbelief.get(), null);
	}
	
	@Test
	public void testSilentValBeanBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
				
		// Test set of val to new bean.
		Future<IEvent>	fut2	= new Future<>();
		Bean	old = pojo.silentvalbeanbelief.get();
		exta.scheduleStep(() ->
		{
			addEventListenerRule(fut2, ChangeEvent.FACTCHANGED, "silentvalbeanbelief");
			pojo.silentvalbeanbelief.set(new Bean(2));
			return null;
		}).get(TestHelper.TIMEOUT);
		checkEventInfo(fut2, old, pojo.silentvalbeanbelief.get(), null);
		
		// Test set of bean property.
		Future<IEvent>	fut3	= new Future<>();
		exta.scheduleStep(() ->
		{
			addEventListenerRule(fut3, ChangeEvent.FACTCHANGED, "silentvalbeanbelief");
			pojo.silentvalbeanbelief.get().setValue(3);
			return null;
		});
		assertThrows(TimeoutException.class, () -> fut3.get(500)); // No event should be generated
	}

	@Test
	public void testListBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		Future<IEvent>	changedfut	= new Future<>();
		Future<IEvent>	addedfut	= new Future<>();
		Future<IEvent>	removedfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(changedfut, ChangeEvent.FACTCHANGED, "listbelief");
			addEventListenerRule(addedfut, ChangeEvent.FACTADDED, "listbelief");
			addEventListenerRule(removedfut, ChangeEvent.FACTREMOVED, "listbelief");
			pojo.listbelief.set(1, "3");
			pojo.listbelief.add(1, "2");
			pojo.listbelief.remove(1);

		});
		
		checkEventInfo(changedfut, "2", "3", 1);
		checkEventInfo(addedfut, null, "2", 1);
		checkEventInfo(removedfut, null, "2", 1);
	}

	@Test
	public void testSetBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		Future<IEvent>	addedfut	= new Future<>();
		Future<IEvent>	removedfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(addedfut, ChangeEvent.FACTADDED, "setbelief");
			addEventListenerRule(removedfut, ChangeEvent.FACTREMOVED, "setbelief");
			pojo.setbelief.add("2");
			pojo.setbelief.remove("2");

		});
		
		checkEventInfo(addedfut, null, "2", null);
		checkEventInfo(removedfut, null, "2", null);
	}
	
	@Test
	public void testMapBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		Future<IEvent>	changedfut	= new Future<>();
		Future<IEvent>	addedfut	= new Future<>();
		Future<IEvent>	removedfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(changedfut, ChangeEvent.FACTCHANGED, "mapbelief");
			addEventListenerRule(addedfut, ChangeEvent.FACTADDED, "mapbelief");
			addEventListenerRule(removedfut, ChangeEvent.FACTREMOVED, "mapbelief");
			pojo.mapbelief.put("2", "two");
			pojo.mapbelief.put("3", "three");
			pojo.mapbelief.remove("2");

		});
		
		checkEventInfo(changedfut, "wto", "two", "2");
		checkEventInfo(addedfut, null, "three", "3");
		checkEventInfo(removedfut, null, "two", "2");
	}

	@Test
	public void testDynamicBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		Future<Integer>	firstfut	= new Future<>();
		Future<IEvent>	changedfut	= new Future<>();
		Future<Integer>	secondfut	= new Future<>();
//		Future<Void>	exfut	= new Future<>();	// no more set() method for dynamic belief
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(changedfut, ChangeEvent.FACTCHANGED, "dynamicbelief");
			firstfut.setResult(pojo.dynamicbelief.get());
			pojo.valbelief.set(2);
			secondfut.setResult(pojo.dynamicbelief.get());
//			try
//			{
//				pojo.dynamicbelief.set(3);
//				exfut.setResult(null);
//			}
//			catch(Exception e)
//			{
//				exfut.setException(e);
//			}
		});
		
		assertEquals(2, firstfut.get(TestHelper.TIMEOUT));
		assertEquals(3, secondfut.get(TestHelper.TIMEOUT));
//		assertThrows(IllegalStateException.class, ()->exfut.get(TestHelper.TIMEOUT));
		checkEventInfo(changedfut, 2, 3, null);
	}
	
	@Test
	public void testUpdaterateBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IComponentHandle	exta	= IComponentManager.get().create(pojo).get(TestHelper.TIMEOUT);
		Future<Long>	firstfut	= new Future<>();
		Future<Long>	secondfut	= new Future<>();
		Future<IEvent>	changedfut	= new Future<>();
		Future<Long>	thirdfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(changedfut, ChangeEvent.FACTCHANGED, "updatebelief");
			firstfut.setResult(pojo.updatebelief.get());
			IExecutionFeature.get().waitForDelay(500).get();
			secondfut.setResult(pojo.updatebelief.get());
			IExecutionFeature.get().waitForDelay(1000).get();
			thirdfut.setResult(pojo.updatebelief.get());
		});
		
		assertNotNull(firstfut.get(TestHelper.TIMEOUT));
		assertEquals(firstfut.get(TestHelper.TIMEOUT), secondfut.get(TestHelper.TIMEOUT));
		assertNotEquals(firstfut.get(TestHelper.TIMEOUT), thirdfut.get(TestHelper.TIMEOUT));
		changedfut.get(TestHelper.TIMEOUT);	// Check if event was generated
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
				.addBeliefListener("valbelief", new IBeliefListener<Integer>()
			{
				@Override
				public void factChanged(ChangeInfo<Integer> info)
				{
					facts.add(info.getOldValue()+":"+info.getValue());
				}
			});
			
			pojo.valbelief.set(1);
			pojo.valbelief.set(2);
			pojo.valbelief.set(3);
			
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("1:2", "2:3"), facts);

		// Test fact added/removed
		List<String>	changes	= new ArrayList<>();
		exta.scheduleStep(comp -> {
			comp.getFeature(IBDIAgentFeature.class)
				.addBeliefListener("setbelief", new IBeliefListener<String>()
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
			
			pojo.setbelief.add("1");	// already contained
			pojo.setbelief.add("2");
			pojo.setbelief.remove("1");
			
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("a:2", "r:1"), changes);

		// Test map belief
		List<String>	mchanges	= new ArrayList<>();
		exta.scheduleStep(comp -> {
			comp.getFeature(IBDIAgentFeature.class)
				.addBeliefListener("mapbelief", new IBeliefListener<String>()
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
			
			pojo.mapbelief.put("2", "two");	// already contained
			pojo.mapbelief.put("3", "three");
			pojo.mapbelief.remove("1");
			
			return null;
		}).get(TestHelper.TIMEOUT);
		assertEquals(Arrays.asList("c:2;two", "a:3;three", "r:1;one"), mchanges);
	}
	
	//-------- helper methods --------
	
	static int	rulecnt	= 0;
	
	/**
	 *  Add a rule that sets the event into a future.
	 *  Must be called on agent thread.
	 */
	public static void	addEventListenerRule(Future<IEvent> fut, String... events)
	{
		BDIAgentFeature	feat	= (BDIAgentFeature)IComponentManager.get().getCurrentComponent()
			.getFeature(IBDIAgentFeature.class);
		feat.getRuleSystem().getRulebase().addRule(new Rule<Void>(
			"EventListenerRule"+Arrays.toString(events)+"_"+rulecnt++,	// Rule Name
			event -> new Future<>(new Tuple2<Boolean, Object>(true, null)),	// Condition -> true
			(event, rule, context, condresult) -> {fut.setResultIfUndone(event); return IFuture.DONE;}, // Action -> set future
			new EventType[] {new EventType(events)}	// Trigger Event(s)
		));
	}
	
	/**
	 *  Check if old/new value and info match expectations.
	 */
	public static void checkEventInfo(Future<IEvent> fut, Object oldval, Object newval, Object info)
	{
		IEvent	event	= fut.get(TestHelper.TIMEOUT);
		@SuppressWarnings("unchecked")
		ChangeInfo<Object>	ci	= (ChangeInfo<Object>)event.getContent();
		assertEquals(oldval, ci.getOldValue(), "old value");
		assertEquals(newval, ci.getValue(), "new value");
		assertEquals(info, ci.getInfo(), "info");
	}
}
