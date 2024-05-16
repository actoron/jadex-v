package jadex.bdi.beliefs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import org.junit.jupiter.api.Test;

import jadex.bdi.annotation.Belief;
import jadex.bdi.runtime.IBDIAgent;
import jadex.bdi.runtime.Val;
import jadex.bdi.runtime.impl.IInternalBDIAgentFeature;
import jadex.common.Tuple2;
import jadex.core.IExternalAccess;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.Rule;

/**
 *  Test events from all kinds of beliefs.
 */
public class BeliefTest
{
	@Agent(type="bdip")
	static class BeliefTestAgent
	{
		@Belief
		Val<Integer>	valbelief	= new Val<>(1);
		
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
		
		int cnt=0;
		@Belief
		Val<Integer>	dynamicbelief	= new Val<>(()->++cnt);

		@Belief(updaterate = 1000)
		Val<Long>	updatebelief	= new Val<>(()->System.currentTimeMillis());

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
		IExternalAccess	exta	= IBDIAgent.create(pojo);
		Future<IEvent>	fut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(fut, "beliefchanged", "valbelief");
			pojo.valbelief.set(2);
		});
		
		checkEventInfo(fut, 1, 2, null);
	}

	@Test
	public void testBeanBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IExternalAccess	exta	= IBDIAgent.create(pojo);
		Future<IEvent>	fut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(fut, "factchanged", "beanbelief");
			pojo.beanbelief.setValue(2);
		});
		
		checkEventInfo(fut, 1, 2, null);
	}

	@Test
	public void testListBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IExternalAccess	exta	= IBDIAgent.create(pojo);
		Future<IEvent>	changedfut	= new Future<>();
		Future<IEvent>	addedfut	= new Future<>();
		Future<IEvent>	removedfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(changedfut, "factchanged", "listbelief");
			addEventListenerRule(addedfut, "factadded", "listbelief");
			addEventListenerRule(removedfut, "factremoved", "listbelief");
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
		IExternalAccess	exta	= IBDIAgent.create(pojo);
		Future<IEvent>	addedfut	= new Future<>();
		Future<IEvent>	removedfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(addedfut, "factadded", "setbelief");
			addEventListenerRule(removedfut, "factremoved", "setbelief");
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
		IExternalAccess	exta	= IBDIAgent.create(pojo);
		Future<IEvent>	changedfut	= new Future<>();
		Future<IEvent>	addedfut	= new Future<>();
		Future<IEvent>	removedfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			addEventListenerRule(changedfut, "factchanged", "mapbelief");
			addEventListenerRule(addedfut, "factadded", "mapbelief");
			addEventListenerRule(removedfut, "factremoved", "mapbelief");
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
		IExternalAccess	exta	= IBDIAgent.create(pojo);
		Future<Integer>	firstfut	= new Future<>();
		Future<Integer>	secondfut	= new Future<>();
		Future<Void>	exfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			firstfut.setResult(pojo.dynamicbelief.get());
			secondfut.setResult(pojo.dynamicbelief.get());
			try
			{
				pojo.dynamicbelief.set(3);
				exfut.setResult(null);
			}
			catch(Exception e)
			{
				exfut.setException(e);
			}
		});
		
		assertEquals(1, firstfut.get(1000));
		assertEquals(2, secondfut.get(1000));
		assertThrows(IllegalStateException.class, ()->exfut.get(1000));
	}
	
	@Test
	public void testUpdaterateBelief()
	{
		BeliefTestAgent	pojo	= new BeliefTestAgent();
		IExternalAccess	exta	= IBDIAgent.create(pojo);
		Future<Long>	firstfut	= new Future<>();
		Future<Long>	secondfut	= new Future<>();
		Future<Long>	thirdfut	= new Future<>();
		
		exta.scheduleStep(() ->
		{
			firstfut.setResult(pojo.updatebelief.get());
			IExecutionFeature.get().waitForDelay(500).get();
			secondfut.setResult(pojo.updatebelief.get());
			IExecutionFeature.get().waitForDelay(1000).get();
			thirdfut.setResult(pojo.updatebelief.get());
		});
		
		assertEquals(firstfut.get(1000), secondfut.get(1000));
		assertNotEquals(firstfut.get(1000), thirdfut.get(2000));
	}

	//-------- helper methods --------
	
	/**
	 *  Add a rule that sets the event into a future.
	 *  Must be called on agent thread.
	 */
	public static void	addEventListenerRule(Future<IEvent> fut, String... events)
	{
		IInternalBDIAgentFeature	feat	= IInternalBDIAgentFeature.get();
		feat.getRuleSystem().getRulebase().addRule(new Rule<Void>(
			"EventListenerRule"+Arrays.toString(events),	// Rule Name
			event -> new Future<>(new Tuple2<Boolean, Object>(true, null)),	// Condition -> true
			(event, rule, context, condresult) -> {fut.setResult(event); return IFuture.DONE;}, // Action -> set future
			new EventType[] {new EventType(events)}	// Trigger Event(s)
		));
	}
	
	/**
	 *  Check if old/new value and info match expectations.
	 */
	public static void checkEventInfo(Future<IEvent> fut, Object oldval, Object newval, Object info)
	{
		IEvent	event	= fut.get(1000);
		@SuppressWarnings("unchecked")
		ChangeInfo<Object>	ci	= (ChangeInfo<Object>)event.getContent();
		assertEquals(oldval, ci.getOldValue(), "old value");
		assertEquals(newval, ci.getValue(), "new value");
		assertEquals(info, ci.getInfo(), "info");
	}
}
