package jadex.bt.impl;

import java.beans.PropertyChangeEvent;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jadex.bt.IBTProvider;
import jadex.bt.Val;
import jadex.bt.actions.UserBaseAction;
import jadex.bt.decorators.ConditionalDecorator;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.collection.ListWrapper;
import jadex.collection.MapWrapper;
import jadex.collection.SetWrapper;
import jadex.common.IResultCommand;
import jadex.common.ITriFunction;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.execution.IExecutionFeature;
import jadex.execution.future.FutureFunctionality;
import jadex.execution.impl.ComponentTimerCreator;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.execution.impl.ILifecycle;
import jadex.future.Future;
import jadex.future.FutureHelper;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.micro.impl.MicroAgentFeature;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.EventType;
import jadex.rules.eca.Rule;
import jadex.rules.eca.RuleEvent;
import jadex.rules.eca.RuleSystem;

public class BTAgentFeature	extends MicroAgentFeature implements ILifecycle
{
	static Field valvalue = SReflect.getField(Val.class, "value");
	//static Field valpojo = SReflect.getField(Val.class, "pojo");
	//static Field valname = SReflect.getField(Val.class, "name");
	static Field valupd = SReflect.getField(Val.class, "updaterate");
	static Field valdyn = SReflect.getField(Val.class, "dynamic");
	static Method valinit = SReflect.getAllMethods(Val.class, "init")[0];
	{
		valvalue.setAccessible(true);
		//valpojo.setAccessible(true);
		//valname.setAccessible(true);
		valupd.setAccessible(true);
		valdyn.setAccessible(true);
		valinit.setAccessible(true);
	}
	
	/** Event type that a value has been added. */
	public static final String VALUEADDED = "valueadded";
	
	/** Event type that a value has been removed. */
	public static final String VALUEREMOVED = "valueremoved";

	/** Event type that a value has changed (property change in case of bean). */
	public static final String PROPERTYCHANGED = "propertychanged";

	/** Event type that a parameter value has changed (the whole value was changed). */
	public static final String VALUECHANGED = "valuechanged";

	
	/** The behavior tree. */
	protected Node<IComponent> bt;

	/** The execution context. */
	protected ExecutionContext<IComponent> context;
	
	/** The rule system. */
	protected RuleSystem rulesystem;
	
	/** The event adders. */
	protected Map<EventType, IResultCommand<IFuture<Void>, PropertyChangeEvent>> eventadders 
		= new HashMap<EventType, IResultCommand<IFuture<Void>,PropertyChangeEvent>>();

	public static BTAgentFeature get()
	{
		Object o = IExecutionFeature.get().getComponent().getFeature(MicroAgentFeature.class);
		//if(!(o instanceof BTAgentFeature))
		//	System.out.println("sdgjhfjhsdgfjh");
		return (BTAgentFeature)IExecutionFeature.get().getComponent().getFeature(MicroAgentFeature.class);
	}

	protected BTAgentFeature(BTAgent self)
	{
		super(self);
	}
	
	@Override
	public void	onStart()
	{
		IBTProvider prov = (IBTProvider)getSelf().getPojo();
		
		this.bt = prov.createBehaviorTree();
		//System.out.println("createBehaviorTree");
		
		this.context = createExecutionContext();
		
		this.rulesystem = new RuleSystem(self.getPojo(), true);
		//System.out.println("createRuleSystem");
		
		// step listener not working for async steps
		// now done additionally after action node actions
		((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).addStepListener(new BTStepListener());
		//System.out.println("createStepLis");
		
		super.onStart();
		//System.out.println("super on start");
		
		initRulesystem();
		
		//System.out.println("init rule system: "+IExecutionFeature.get().getComponent());
		
		initVals();
		
		//System.out.println("init vals");
		
		executeBehaviorTree(bt, null);
		//System.out.println("execute bt");
		
		/*getSelf().getFeature(IExecutionFeature.class).scheduleStep(() -> 
		{
			executeBehaviorTree(bt, null);
			System.out.println("execute bt");
			return IFuture.DONE;
		}).catchEx(e -> getSelf().handleException(e));*/
	}
	
	protected void initVals()
	{
		Object pojo = self.getPojo();
		
		Field[] fields = SReflect.getAllFields(pojo.getClass());
		
		for(Field field: fields)
		{
			try
			{
				field.setAccessible(true);
				Object val	= field.get(pojo);
				//boolean dowrite = val!=null;
				
				if(val==null && SReflect.isSupertype(Val.class, field.getType()))
				{
					val	= new Val(null);
					field.set(pojo, val);
				}
				
				// Lazy init of belief wrapper
				if(val instanceof Val<?>)
				{
					try
					{
						valinit.invoke(val, pojo, field.getName());
						//valpojo.set(val, pojo);
						//valname.set(val, field.getName());
					
						/*Long updaterate = (Long)valupd.get(val);
						Callable<Object> dynamic = (Callable<Object>)valdyn.get(val);
						if(updaterate!=null && dynamic!=null)
						{
							System.getLogger(getClass().getName()).log(Level.INFO, "activating dynamic value: "+val);
							//System.out.println("activating dynamic value: "+val);
							IThrowingConsumer<IComponent> task[] = new IThrowingConsumer[1];
							task[0] = agent ->
							{
								Object res = dynamic.call();
								BTAgentFeature.writeField(res, field.getName(), pojo, MicroAgentFeature.get().getSelf());

								//System.out.println("update value: "+res);
								IExecutionFeature.get().waitForDelay(updaterate).then(Void -> 
								{
									try
									{
										task[0].accept(agent);
									}
									catch(Exception e)
									{
										e.printStackTrace();
									}
								}).printOnEx();
							};
							IExecutionFeature.get().scheduleStep(task[0]);
							//IExecutionFeature.get().waitForDelay(updaterate)
							//	.then(Void -> IExecutionFeature.get().scheduleStep(task[0]))
							//	.printOnEx();
						}*/
					}
					catch(Exception e)
					{
						SUtil.throwUnchecked(e);
					}
					
					//dowrite	= ((Val)val).get()!=null;
				
					//if(dowrite)
					//	writeField(val, field.getName(), pojo);
				}
				
				// todo: support non-val observables
				
				//if(dowrite)
				//	writeField(val, field.getName(), MicroAgentFeature.get().getSelf());
				
				Object wval = wrapMultiValue(val, field.getName());
				if(wval!=null && wval!=val)
				{
					field.set(pojo, wval);
					//System.out.println("set wrapped value: "+field.getName());
				}
			}
			catch(Exception e)
			{
				SUtil.throwUnchecked(e);
			}
		}
	}
	
	protected void initRulesystem()
	{
		Collection<Node<IComponent>> nodes = new ArrayList<>();
		bt.collectNodes(nodes);
		
		nodes.stream().forEach(node ->
		{
			if(node instanceof ActionNode<IComponent>)
			{
				ActionNode<IComponent> an = (ActionNode<IComponent>)node;
				
				UserBaseAction<IComponent, ? extends IFuture<NodeState>> action = an.getAction();
				
				BiFunction<Event, IComponent, ? extends IFuture<NodeState>> action2 = action.getAction();
				
				// todo: how can this be done with generics?
				//action.setAction((BiFunction<Event, IComponent, ? extends IFuture<NodeState>>)(e, agent) ->
				action.setAction((BiFunction)(e, agent) ->
				{
					//Exception ex = new RuntimeException();
					
					IFuture<NodeState>[] stepret = new IFuture[1]; 
					stepret[0] = ((IComponent)agent).getFeature(IExecutionFeature.class).scheduleAsyncStep(new IThrowingFunction<IComponent, IFuture<NodeState>>() 
					{
						@Override
						public IFuture<NodeState> apply(IComponent comp) throws Exception 
						{
							if(stepret[0]!=null && stepret[0].isDone())
							{
								System.getLogger(getClass().getName()).log(Level.INFO, "action omitted: "+node);
								//System.out.println("action omitted: "+node);
								Future<NodeState> donefut = Future.getFuture(getFutureReturnType());
								stepret[0].delegateTo(donefut);
								return donefut;
							}
							
							//ex.printStackTrace();
							
							IFuture<NodeState> ret = action2.apply((Event)e, (IComponent)agent);
							
							Future<NodeState> ret2 = (Future<NodeState>)FutureFunctionality.getDelegationFuture(ret.getClass(), new FutureFunctionality()
							{
								public Object handleResult(Object result) throws Exception
								{
									executeRulesystem();
									return result;
								}
								
								public void	handleException(Exception exception)
								{
									executeRulesystem();
								}
							});
							
							ret.delegateTo(ret2);
							
							/*ret.then(s -> 
							{
								executeRulesystem();
								ret2.setResult(s);
							}).catchEx(ex ->
							{
								executeRulesystem();
								ret2.setException(ex);
							});*/
							
							return ret2;
						}
						
						@Override
						public Class<? extends IFuture<?>> getFutureReturnType() 
						{
							//if(action.getDescription().indexOf("pickup")!=-1)
							//	System.out.println("RETURNTYPE: action: "+action.getFutureReturnType()+" "+action.getDescription());
							return action.getFutureReturnType();
						}
					});
					
					return stepret[0];
				});
			}
			
			List<ConditionalDecorator<IComponent>> cdecos = node.getDecorators().stream()
				.filter(deco -> deco instanceof ConditionalDecorator)
				.map(deco -> (ConditionalDecorator<IComponent>)deco)
				.collect(Collectors.toList());
			
			for(ConditionalDecorator<IComponent> deco: cdecos)
			{
				if(deco.getAction()==null || deco.getEvents()==null || deco.getEvents().length==0)
				{
					if(deco.getCondition()!=null)
					{
						System.getLogger(getClass().getName()).log(Level.INFO, "skipping condition for deco: "+deco);
						//System.out.println("skipping condition for deco: "+deco);
					}
				}
				else
				{
					//System.out.println("rulename: "+deco.toString()+"_"+node.getId());
					rulesystem.getRulebase().addRule(new Rule<Void>(
						deco.toString()+"_"+node.getId(), 
						e -> // condition
						{
							Future<Tuple2<Boolean, Object>> ret = new Future<>();
							if(deco.getCondition()!=null)
							{
								NodeContext<IComponent> context = node.getNodeContext(getExecutionContext());
								ITriFunction<Event, NodeState, ExecutionContext<IComponent>, IFuture<Boolean>> cond = deco.getCondition();
								IFuture<Boolean> fut = cond.apply(new Event(e.getType().toString(), e.getContent()), context!=null? context.getState(): NodeState.IDLE, getExecutionContext());
								fut.then(triggered ->
								{
									ret.setResult(new Tuple2<>(triggered, null));
								}).catchEx(ex -> 
								{
									ret.setResult(new Tuple2<>(false, null));
								});
							}
							else if(deco.getFunction()!=null)
							{
								NodeContext<IComponent> context = node.getNodeContext(getExecutionContext());
								ITriFunction<Event, NodeState, ExecutionContext<IComponent>, IFuture<NodeState>> cond = deco.getFunction();
								IFuture<NodeState> fut = cond.apply(new Event(e.getType().toString(), e.getContent()), context!=null? context.getState(): NodeState.IDLE, getExecutionContext());
								fut.then(state ->
								{
									ret.setResult(new Tuple2<>(deco.mapToBoolean(state), null));
								}).catchEx(ex -> 
								{
									ret.setResult(new Tuple2<>(false, null));
								});
							}
							else
							{
								System.getLogger(getClass().getName()).log(Level.WARNING, "Rule without condition: "+deco);
								//System.out.println("Rule without condition: "+deco);
								ret.setResult(new Tuple2<>(false, null));
							}
							return ret;
						}, 
						deco.getAction(), deco.getEvents() // trigger events
					));
				}
			}
		});
	}
	
	protected static void executeRulesystem()
	{
		System.getLogger(BTAgentFeature.class.getName()).log(Level.INFO, "executeRulesystem");
		//System.out.println("executeRulesystem");
		BTAgentFeature btf = BTAgentFeature.get();
		
		//Set<Node<IComponent>> nodes = new HashSet<Node<IComponent>>();
		while(btf.getRuleSystem()!=null && btf.getRuleSystem().isEventAvailable())
		{
			//System.out.println("executeCycle.PAE start: "+btf.getRuleSystem().getEventCount());
			IIntermediateFuture<RuleEvent> res = btf.getRuleSystem().processEvent();
			FutureHelper.notifyStackedListeners();
			if(!res.isDone())
			{
				System.getLogger(BTAgentFeature.class.getName()).log(Level.ERROR, "No async actions allowed.");
				//System.err.println("No async actions allowed.");
			}
			//res.get().stream().forEach(re -> nodes.add((Node<IComponent>)re.getResult()));*/
			
			//IFuture<Void> fut = btf.getRuleSystem().processAllEvents();
			//if(!fut.isDone())
			//	System.err.println("No async actions allowed.");
		}
		
		/*Set<Node<IComponent>> parents = new HashSet<Node<IComponent>>();
		if(!nodes.isEmpty())
		{
			nodes.stream().forEach(node ->
			{
				if(node.getParent()!=null && node.getParent().get)
			});
		}*/
	}
	
	protected ExecutionContext<IComponent> createExecutionContext()
	{
		ExecutionContext<IComponent> ret = new ExecutionContext<IComponent>();
		ret.setUserContext(self);
		ret.setTimerCreator(new ComponentTimerCreator());
		return ret;
	}
	
	public ExecutionContext<IComponent> getExecutionContext()
	{
		return context;
	}
	
	public void executeBehaviorTree(Node<IComponent> node, Event event)
	{
		// new execution
		if(context==null || context.getRootCall()==null || context.getRootCall().isDone())
		{
			context = createExecutionContext();
			
			IFuture<NodeState> call = bt.execute(event!=null? event: new Event("start", null), context);
			
			call.then(state -> 
			{
				System.getLogger(this.getClass().getName()).log(Level.INFO, "final state: "+context+" "+state);
				//System.out.println("final state: "+context+" "+state);
				
				IComponentManager.get().terminate(getSelf().getId());
				
				// todo: support repeat mode of node
				/*if(NodeState.FAILED.equals(state))
				{
					getSelf().getFeature(IExecutionFeature.class).scheduleStep(() -> 
					{
						IFuture<Void> fut = rulesystem.processAllEvents();
						if(!fut.isDone())
							System.err.println("No async actions allowed.");
						
						executeBehaviorTree(bt, event);
					});
				}*/
			});
		}
		// ongoing execution
		else if(context.getRootCall()!=null && !context.getRootCall().isDone())
		{
			if(node==null)
				node = bt;
			
			// Find the active parent
			Node<IComponent> parent = getActiveParent(node);
			if(parent!=null)
			{
				parent.execute(event, context);
			}
			else
			{
				throw new RuntimeException("Context has unfinished root call but no active node - should not occur: "+node);
			}
		}
	}
	
	protected Node<IComponent> getActiveParent(Node<IComponent> node)
	{
		// Find the active parent
		Node<IComponent> parent = node.getParent();
		boolean found = false;
		
		while(parent!=null && !found)
		{
			if(context.getNodeContext(parent)!=null)
			{
				NodeContext<IComponent> nc = context.getNodeContext(parent);
				if(NodeState.RUNNING==nc.getState())
				{
					found = true;
				}
				else if(nc.getCallFuture()!=null && !nc.getCallFuture().isDone() && nc.getState()==null)
				{
					found = true;
					System.getLogger(this.getClass().getName()).log(Level.WARNING, "found parent with open call but state: "+parent+" "+nc.getState());
					//System.out.println("found parent with open call but state: "+parent+" "+nc.getState());
				}
			}
			else
			{
				parent = parent.getParent();
			}
		}

		return parent;
	}
	
	/*public boolean resetOngoingExecution(Node<IComponent> node, ExecutionContext<IComponent> context)
	{
		boolean ret = false;
		
		// ongoing execution
		if(node!=null && context.getRootCall()!=null && !context.getRootCall().isDone())
		{
			Node<IComponent> parent = getActiveParent(node);
			if(parent!=null)
			{
				NodeState state = context.getNodeContext(parent).getState();
				if(NodeState.RUNNING==state || state==null)
				{
					System.getLogger(this.getClass().getName()).log(Level.INFO, "resetting node and aborting its children: "+parent+" "+state);
					//System.out.println("resetting node and aborting its children: "+parent+" "+state);
					parent.reset(context, false);
					parent.abort(AbortMode.SUBTREE, NodeState.FAILED, context);
					ret = true;
				}
				else
				{
					System.getLogger(this.getClass().getName()).log(Level.WARNING, "found active parent that is not running, should not happen: "+parent+" "+state);
					//System.out.println("found active parent that is not running, should not happen: "+parent+" "+state);
				}
			}
			else
			{
				System.getLogger(this.getClass().getName()).log(Level.INFO, "no active parent found for node: "+node);
				//System.out.println("no active parent found for node: "+node);
			}
		}
		
		return ret;
	}*/
	
	public boolean resetOngoingExecution(Node<IComponent> node, ExecutionContext<IComponent> context) 
	{
	    if (node == null || context.getRootCall() == null || context.getRootCall().isDone()) 
	        return false; 

	    Node<IComponent> parent = getActiveParent(node);
	    if (parent == null) 
	    {
	        System.getLogger(this.getClass().getName()).log(Level.INFO, "No active parent found for node: " + node);
	        return false;
	    }

	    NodeContext<IComponent> pacontext = context.getNodeContext(parent);
	    NodeState state = pacontext.getState();

	    if (state != NodeState.RUNNING && state != null) 
	    {
	        System.getLogger(this.getClass().getName()).log(Level.WARNING,
	            "Found active parent that is not running, should not happen: " + parent + " " + state);
	        return false;
	    }

	    // Logging der Resets und Aborts
	    System.getLogger(this.getClass().getName()).log(Level.INFO,
	        "Resetting node and aborting its children: " + parent + " " + state);

	    //pacontext.reset(false);
	    parent.reset(context, false);

	    // reset parent and reexecute
	    parent.abort(AbortMode.SUBTREE, NodeState.FAILED, context).get();
	    
	    parent.reset(context, false);
	    return true;
	}
	
	/**
	 *  Get the rulesystem.
	 *  @return The rulesystem.
	 */
	public RuleSystem getRuleSystem()
	{
		return rulesystem;
	}
	
	// Wrap collections of multi vals (if not already a wrapper)
	public static Object wrapMultiValue(Object val, String fieldname)
	{
		// Wrap collections of multi beliefs (if not already a wrapper)
		EventType addev = new EventType(VALUEADDED, fieldname);
		EventType remev = new EventType(VALUEREMOVED, fieldname);
		EventType chev = new EventType(PROPERTYCHANGED, fieldname);
		
		if(val instanceof List && !(val instanceof jadex.collection.ListWrapper))
		{
			val = new ListWrapper((List<?>)val, new EventPublisher(IExecutionFeature.get().getComponent(), addev, remev, chev));
		}
		else if(val instanceof Set && !(val instanceof jadex.collection.SetWrapper))
		{
			val = new SetWrapper((Set<?>)val, new EventPublisher(IExecutionFeature.get().getComponent(), addev, remev, chev));
		}
		else if(val instanceof Map && !(val instanceof jadex.collection.MapWrapper))
		{
			val = new MapWrapper((Map<?,?>)val, new EventPublisher(IExecutionFeature.get().getComponent(), addev, remev, chev));
		}
		
		return val;
	}
	
	public static void writeField(Object val, String fieldname, Object obj, IComponent comp)
	{
		val = wrapMultiValue(val, fieldname);
		
		// agent is not null any more due to deferred exe of init expressions but rules are
		// available only after startBehavior
		//if(((IInternalBDILifecycleFeature)MicroAgentFeature.get()).isInited())
		//{
		((BTAgentFeature)BTAgentFeature.get()).writeField(val, fieldname, obj);
		//}
		
		// Only store event for non-update-rate beliefs (update rate beliefs get set later)
//		else if(mbel.getUpdaterate()<=0)
		/*else if(mbel.getUpdateRate()==null)
		{
			// In init set field immediately but throw events later, when agent is available.
			
			try
			{
				Object oldval = setFieldValue(obj, fieldname, val);
				// rule engine not turned on so no unobserve necessary
//				unobserveObject(agent, obj, etype, rs);
				addInitWrite(IExecutionFeature.get().getComponent(), new InitWriteBelief(mbel.getName(), val, oldval));
			}
			catch(Exception e)
			{
				SUtil.throwUnchecked(e);
			}
		}*/
	}
	
	protected void writeField(Object val, String fieldname, Object obj)
	{
		writeField(val, fieldname, obj, new EventType(VALUECHANGED, fieldname), new EventType(PROPERTYCHANGED, fieldname));
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as field access.
	 */
	protected void writeField(Object val, String fieldname, Object obj, EventType ev1, EventType ev2)
	{
		//assert isComponentThread();
		
		// todo: support for belief sets (un/observe values? insert mappers when setting value etc.
		
		try
		{
//			System.out.println("write: "+val+" "+fieldname+" "+obj);
//			BDIAgentInterpreter ip = (BDIAgentInterpreter)getInterpreter();
			RuleSystem rs = getRuleSystem();

			Object oldval = getFieldValue(obj, fieldname, null, false);
			if(oldval instanceof Val)
			{
				Val<?> valc = (Val<?>)oldval;
				oldval = valvalue.get(valc);
				valvalue.set(valc, val);
			}
			else
			{
				setFieldValue(obj, fieldname, val);
			}
			
			// unobserve old value for property changes
			unobserveObject(oldval, ev2, rs, eventadders);
//			rs.unobserveObject(oldval);

			//MBelief	mbel = ((MCapability)IInternalBDIAgentFeature.get().getCapability().getModelElement()).getBelief(belname);
		
			if(!SUtil.equals(val, oldval))
			{
				//publishToolBeliefEvent(mbel);
				rs.addEvent(new jadex.rules.eca.Event(ev1, new ChangeInfo<Object>(val, oldval, null)));
				
				// execute rulesystem immediately to ensure that variable values are not changed afterwards
				//if(rs.isQueueEvents() && ((IInternalBDILifecycleFeature)MicroAgentFeature.get()).isInited())
				//{
//					System.out.println("writeField.PAE start");
				//  rs.processAllEvents();
				//}
			}
			
			// observe new value for property changes
			observeValue(rs, val, ev2, ev1, eventadders);
			
			// initiate a step to reevaluate the conditions
			//TODO???
//			((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).wakeup();
//			self.getComponentFeature(IExecutionFeature.class).scheduleStep(new ImmediateComponentStep()
//			{
//				public IFuture execute(IInternalAccess ia)
//				{
//					return IFuture.DONE;
//				}
//			});
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Set the value of a field.
	 *  @param obj The object.
	 *  @param fieldname The name of the field.
	 *  @return The old field value.
	 */
	protected static Object setFieldValue(Object obj, String fieldname, Object val) throws IllegalAccessException
	{
		return getFieldValue(obj, fieldname, val, true);
	}
	
	/**
	 *  Set the value of a field.
	 *  @param obj The object.
	 *  @param fieldname The name of the field.
	 *  @return The old field value.
	 */
	protected static Object getFieldValue(Object obj, String fieldname, Object val, boolean set) throws IllegalAccessException
	{
		Tuple2<Field, Object> res = findFieldWithOuterClass(obj, fieldname, false);
		Field f = res.getFirstEntity();
		if(f==null)
			throw new RuntimeException("Field not found: "+fieldname);
		
		Object tmp = res.getSecondEntity();
		SAccess.setAccessible(f, true);
		Object oldval = f.get(tmp);
		if(set)
		{
			f.set(tmp, val);
		}
	
		return oldval;
	}
	
	/**
	 * 
	 * @param obj
	 * @param fieldname
	 * @return
	 */
	protected static Tuple2<Field, Object> findFieldWithOuterClass(Object obj, String fieldname, boolean nonnull)
	{
		Field f = null;
		Object tmp = obj;
		while(f==null && tmp!=null)
		{
			//f = findFieldWithSuperclass(tmp.getClass(), fieldname, obj, nonnull);
			f = findFieldWithSuperclass(tmp.getClass(), fieldname, tmp, nonnull);
			
			// If field not found try searching outer class
			if(f==null)
			{
				try
				{
					// Does not work in STATIC (i think?) inner inner classes $1 $2 ...
					// because __agent cannot be accessed :(
					// TODO: static inner classes may need __agent field!
//					Field fi = tmp.getClass().getDeclaredField("this$0");
					Field[] fs = tmp.getClass().getDeclaredFields();
					boolean found = false;
					for(Field fi: fs)
					{
						if(fi.getName().startsWith("this$"))
						{
							SAccess.setAccessible(fi, true);
							tmp = fi.get(tmp);
							found = true;
							break;
						}
					}
					if(!found)
						tmp = null;
				}
				catch(Exception e)
				{
//					e.printStackTrace();
					tmp=null;
				}
			}
		}
		return f!=null ? new Tuple2<Field, Object>(f, tmp) : null;
	}
	
	/**
	 * 
	 * @param cl
	 * @param fieldname
	 * @return
	 */
	protected static Field findFieldWithSuperclass(Class<?> cl, String fieldname, Object obj, boolean nonnull)
	{
		//final Class<?> fcl = cl;
		//System.out.println("findField"+fcl);
		
		Field ret = null;
		while(ret==null && !Object.class.equals(cl))
		{
			try
			{
				ret = cl.getDeclaredField(fieldname);
				if(nonnull && ret!=null)
				{
					SAccess.setAccessible(ret, true);
					if(ret.get(obj)==null)
						ret	= null;
				}
			}
			catch(Exception e)
			{
			}
			cl = cl.getSuperclass();
		}
		return ret;
	}
	
	/**
	 *  Unobserve an object.
	 */
	public static void unobserveObject(final Object object, EventType etype, RuleSystem rs, Map<EventType, IResultCommand<IFuture<Void>, PropertyChangeEvent>> eventadders)
	{
		IResultCommand<IFuture<Void>, PropertyChangeEvent> eventadder = eventadders.get(etype);
		rs.unobserveObject(object, eventadder);
	}
	
	/**
	 * 
	 */
	protected static synchronized IResultCommand<IFuture<Void>, PropertyChangeEvent> getEventAdder(final EventType etype, final EventType etypeobj,
		final RuleSystem rs, Map<EventType, IResultCommand<IFuture<Void>, PropertyChangeEvent>> eventadders)
	{
		IResultCommand<IFuture<Void>, PropertyChangeEvent> ret = eventadders.get(etype);
		
		if(ret==null)
		{
			ret = new IResultCommand<IFuture<Void>, PropertyChangeEvent>()
			{
				final IResultCommand<IFuture<Void>, PropertyChangeEvent> self = this;
				public IFuture<Void> execute(final PropertyChangeEvent event)
				{
					final Future<Void> ret = new Future<Void>();
					try
					{
						//publishToolBeliefEvent(mbel);
						EventType proptype = etype;
						String propname = event.getPropertyName();
						if("general".equals(propname))
						{
							proptype = etypeobj;
						}
						else if(propname!=null)
						{
							String[] combined = Stream.concat(Stream.of(etype.getTypes()), Stream.of(propname)).toArray(String[]::new);
							proptype = new EventType(combined);
						}
						jadex.rules.eca.Event ev = new jadex.rules.eca.Event(proptype, new ChangeInfo<Object>(event.getNewValue(), event.getOldValue(), null));
						rs.addEvent(ev);
					}
					catch(Exception e)
					{
						if(!(e instanceof ComponentTerminatedException))
							System.err.println("Ex in observe: "+SUtil.getExceptionStacktrace(e));
						Object val = event.getSource();
						rs.unobserveObject(val, self);
						ret.setResult(null);
					}
					return ret;
				}
			};
			eventadders.put(etype, ret);
		}
		
		return ret;
	}
	
	/**
	 *  Get the event type.
	 *  @return The event adder.
	 */
	public Map<EventType, IResultCommand<IFuture<Void>, PropertyChangeEvent>> getEventAdders()
	{
		return eventadders;
	}
	
	/**
	 *  Observe a value.
	 */
	public static void observeValue(final RuleSystem rs, final Object val, final EventType etype, final EventType etypeobj, Map<EventType, IResultCommand<IFuture<Void>, PropertyChangeEvent>> eventadders)
	{
		//assert IExecutionFeature.get().isComponentThread();

		if(val!=null)
			rs.observeObject(val, true, false, getEventAdder(etype, etypeobj, rs, eventadders));
	}

	public Node<IComponent> getBehaviorTree() 
	{
		return bt;
	}
}
