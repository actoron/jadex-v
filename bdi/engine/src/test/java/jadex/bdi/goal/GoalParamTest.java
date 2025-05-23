package jadex.bdi.goal;

/**
 *  Test events from all kinds of goal parameters.
 */
public class GoalParamTest
{
//	@Agent(type="bdip")
//	static class GoalParamTestAgent
//	{
//		@Goal
//		class GoalParamTestGoal
//		{
//			@GoalParameter
//			Val<Integer>	valparam	= new Val<>(1);
//			
//			@GoalParameter
//			Bean	beanparam	= new Bean(1);
//			
//			@GoalParameter
//			List<String>	listparam	= new ArrayList<>(Arrays.asList(new String[]{"1", "2"}));
//			
//			@GoalParameter
//			Set<String>	setparam	= new LinkedHashSet<>(Arrays.asList(new String[]{"1", "3"}));
//
//			@SuppressWarnings("serial")
//			@GoalParameter
//			Map<String, String>	mapparam	= new LinkedHashMap<>()
//			{{
//				put("1", "one");
//				put("2", "wto");
//			}};
//			
////			//TODO: Support dynamic goal parameters!?
////			@GoalParameter(parameters="valparam")
////			Val<Integer>	dynamicparam	= new Val<>(()->valparam.get()+1);
////
////			@GoalParameter(updaterate = 1000)
////			Val<Long>	updateparam	= new Val<>(()->System.currentTimeMillis());			
//		}
//	}
//	
//	public static class Bean
//	{
//		int value;
//		
//		public Bean(int value)
//		{
//			this.value	= value;
//		}
//		
//		PropertyChangeSupport	pcs	= new PropertyChangeSupport(this);
//		public void	addPropertyChangeListener(PropertyChangeListener pcl)
//		{
//			pcs.addPropertyChangeListener(pcl);
//		}
//		
//		public void setValue(int value)
//		{
//			int	oldvalue	= this.value;
//			this.value = value;
//			pcs.firePropertyChange("value", oldvalue, value);
//		}
//	}
//	
//	@Test
//	public void testValParam()
//	{
//		GoalParamTestAgent	pojo	= new GoalParamTestAgent();
//		IComponentHandle	exta	= IBDIAgent.create(pojo);
//		Future<IEvent>	fut	= new Future<>();
//		
//		exta.scheduleStep(agent ->
//		{
//			GoalParamTestAgent.GoalParamTestGoal	goal	= pojo.new GoalParamTestGoal();
//			agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
//			addEventListenerRule(fut, "parameterchanged", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "valparam");
//			goal.valparam.set(2);
//		});
//		
//		checkEventInfo(fut, 1, 2, null);
//	}
//
//	@Test
//	public void testBeanParam()
//	{
//		GoalParamTestAgent	pojo	= new GoalParamTestAgent();
//		IComponentHandle	exta	= IBDIAgent.create(pojo);
//		Future<IEvent>	fut	= new Future<>();
//		
//		exta.scheduleStep(agent ->
//		{
//			GoalParamTestAgent.GoalParamTestGoal	goal	= pojo.new GoalParamTestGoal();
//			agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
//			addEventListenerRule(fut, "valuechanged", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "beanparam");
//			goal.beanparam.setValue(2);
//		});
//		
//		checkEventInfo(fut, 1, 2, null);
//	}
//
//	@Test
//	public void testListParam()
//	{
//		GoalParamTestAgent	pojo	= new GoalParamTestAgent();
//		IComponentHandle	exta	= IBDIAgent.create(pojo);
//		Future<IEvent>	changedfut	= new Future<>();
//		Future<IEvent>	addedfut	= new Future<>();
//		Future<IEvent>	removedfut	= new Future<>();
//		
//		exta.scheduleStep(agent ->
//		{
//			GoalParamTestAgent.GoalParamTestGoal	goal	= pojo.new GoalParamTestGoal();
//			agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
//			addEventListenerRule(changedfut, "valuechanged", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "listparam");
//			addEventListenerRule(addedfut, "valueadded", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "listparam");
//			addEventListenerRule(removedfut, "valueremoved", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "listparam");
//			goal.listparam.set(1, "3");
//			goal.listparam.add(1, "2");
//			goal.listparam.remove(1);
//
//		});
//		
//		checkEventInfo(changedfut, "2", "3", 1);
//		checkEventInfo(addedfut, null, "2", 1);
//		checkEventInfo(removedfut, null, "2", 1);
//	}
//
//	@Test
//	public void testSetParam()
//	{
//		GoalParamTestAgent	pojo	= new GoalParamTestAgent();
//		IComponentHandle	exta	= IBDIAgent.create(pojo);
//		Future<IEvent>	addedfut	= new Future<>();
//		Future<IEvent>	removedfut	= new Future<>();
//		
//		exta.scheduleStep(agent ->
//		{
//			GoalParamTestAgent.GoalParamTestGoal	goal	= pojo.new GoalParamTestGoal();
//			agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
//			addEventListenerRule(addedfut, "valueadded", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "setparam");
//			addEventListenerRule(removedfut, "valueremoved", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "setparam");
//			goal.setparam.add("2");
//			goal.setparam.remove("2");
//
//		});
//		
//		checkEventInfo(addedfut, null, "2", null);
//		checkEventInfo(removedfut, null, "2", null);
//	}
//	
//	@Test
//	public void testMapParam()
//	{
//		GoalParamTestAgent	pojo	= new GoalParamTestAgent();
//		IComponentHandle	exta	= IBDIAgent.create(pojo);
//		Future<IEvent>	changedfut	= new Future<>();
//		Future<IEvent>	addedfut	= new Future<>();
//		Future<IEvent>	removedfut	= new Future<>();
//		
//		exta.scheduleStep(agent ->
//		{
//			GoalParamTestAgent.GoalParamTestGoal	goal	= pojo.new GoalParamTestGoal();
//			agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
//			addEventListenerRule(changedfut, "valuechanged", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "mapparam");
//			addEventListenerRule(addedfut, "valueadded", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "mapparam");
//			addEventListenerRule(removedfut, "valueremoved", GoalParamTestAgent.GoalParamTestGoal.class.getName(), "mapparam");
//			goal.mapparam.put("2", "two");
//			goal.mapparam.put("3", "three");
//			goal.mapparam.remove("2");
//
//		});
//		
//		checkEventInfo(changedfut, "wto", "two", "2");
//		checkEventInfo(addedfut, null, "three", "3");
//		checkEventInfo(removedfut, null, "two", "2");
//	}
//
////	@Test
////	public void testDynamicParam()
////	{
////		GoalParamTestAgent	pojo	= new GoalParamTestAgent();
////		IExternalAccess	exta	= IBDIAgent.create(pojo);
////		Future<Integer>	firstfut	= new Future<>();
////		Future<IEvent>	changedfut	= new Future<>();
////		Future<Integer>	secondfut	= new Future<>();
////		Future<Void>	exfut	= new Future<>();
////		
////		exta.scheduleStep(() ->
////		{
////			addEventListenerRule(changedfut, "paramchanged", "dynamicparam");
////			firstfut.setResult(pojo.dynamicparam.get());
////			pojo.valparam.set(2);
////			secondfut.setResult(pojo.dynamicparam.get());
////			try
////			{
////				pojo.dynamicparam.set(3);
////				exfut.setResult(null);
////			}
////			catch(Exception e)
////			{
////				exfut.setException(e);
////			}
////		});
////		
////		assertEquals(2, firstfut.get(TestHelper.TIMEOUT));
////		assertEquals(3, secondfut.get(TestHelper.TIMEOUT));
////		assertThrows(IllegalStateException.class, ()->exfut.get(TestHelper.TIMEOUT));
////		checkEventInfo(changedfut, 2, 3, null);
////	}
////	
////	@Test
////	public void testUpdaterateParam()
////	{
////		GoalParamTestAgent	pojo	= new GoalParamTestAgent();
////		IExternalAccess	exta	= IBDIAgent.create(pojo);
////		Future<Long>	firstfut	= new Future<>();
////		Future<Long>	secondfut	= new Future<>();
////		Future<IEvent>	changedfut	= new Future<>();
////		Future<Long>	thirdfut	= new Future<>();
////		
////		exta.scheduleStep(() ->
////		{
////			addEventListenerRule(changedfut, "paramchanged", "updateparam");
////			firstfut.setResult(pojo.updateparam.get());
////			IExecutionFeature.get().waitForDelay(500).get();
////			secondfut.setResult(pojo.updateparam.get());
////			IExecutionFeature.get().waitForDelay(1000).get();
////			thirdfut.setResult(pojo.updateparam.get());
////		});
////		
////		assertEquals(firstfut.get(TestHelper.TIMEOUT), secondfut.get(TestHelper.TIMEOUT));
////		assertNotEquals(firstfut.get(TestHelper.TIMEOUT), thirdfut.get(2000));
////		changedfut.get(TestHelper.TIMEOUT);	// Check if event was generated
////	}
//
//	//-------- helper methods --------
//	
//	/**
//	 *  Add a rule that sets the event into a future.
//	 *  Must be called on agent thread.
//	 */
//	public static void	addEventListenerRule(Future<IEvent> fut, String... events)
//	{
//		IInternalBDIAgentFeature	feat	= IInternalBDIAgentFeature.get();
//		feat.getRuleSystem().getRulebase().addRule(new Rule<Void>(
//			"EventListenerRule"+Arrays.toString(events),	// Rule Name
//			event -> new Future<>(new Tuple2<Boolean, Object>(true, null)),	// Condition -> true
//			(event, rule, context, condresult) -> {fut.setResult(event); return IFuture.DONE;}, // Action -> set future
//			new EventType[] {new EventType(events)}	// Trigger Event(s)
//		));
//	}
//	
//	/**
//	 *  Check if old/new value and info match expectations.
//	 */
//	public static void checkEventInfo(Future<IEvent> fut, Object oldval, Object newval, Object info)
//	{
//		IEvent	event	= fut.get(TestHelper.TIMEOUT);
//		@SuppressWarnings("unchecked")
//		ChangeInfo<Object>	ci	= (ChangeInfo<Object>)event.getContent();
//		assertEquals(oldval, ci.getOldValue(), "old value");
//		assertEquals(newval, ci.getValue(), "new value");
//		assertEquals(info, ci.getInfo(), "info");
//	}
}
