package jadex.bt.impl;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jadex.bt.IBTAgentFeature;
import jadex.bt.IBTProvider;
import jadex.bt.decorators.ConditionalDecorator;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.ITriFunction;
import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.ILifecycle;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ComponentTimerCreator;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.future.Future;
import jadex.future.FutureHelper;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.rules.eca.Rule;
import jadex.rules.eca.RuleEvent;
import jadex.rules.eca.RuleSystem;

public class BTAgentFeature implements ILifecycle, IBTAgentFeature
{
	/** Event type that a value has been added. */
	public static final String VALUEADDED = "valueadded";
	
	/** Event type that a value has been removed. */
	public static final String VALUEREMOVED = "valueremoved";

	/** Event type that a value has changed (property change in case of bean). */
	public static final String PROPERTYCHANGED = "propertychanged";

	/** Event type that a parameter value has changed (the whole value was changed). */
	public static final String VALUECHANGED = "valuechanged";

	/** The component. */
	protected BTAgent	self;	
	
	/** The behavior tree. */
	protected Node<IComponent> bt;

	/** The execution context. */
	protected ExecutionContext<IComponent> context;
	
	/** The rule system. */
	protected RuleSystem rulesystem;
	
	public static BTAgentFeature get()
	{
		//Object o = IExecutionFeature.get().getComponent().getFeature(IBTAgentFeature.class);
		//if(!(o instanceof BTAgentFeature))
		//	System.out.println("sdgjhfjhsdgfjh");
		return (BTAgentFeature)IExecutionFeature.get().getComponent().getFeature(IBTAgentFeature.class);
	}

	protected BTAgentFeature(BTAgent self)
	{
		this.self	= self;
	}
	
	public BTAgent	getSelf()
	{
		return self;
	}
	
	@Override
	public void	init()
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
		
		initRulesystem();
		
		//System.out.println("init rule system: "+IExecutionFeature.get().getComponent());
		
		//System.out.println("init vals");
		
		// Done as postInject code in injection feature -> see BTAgentFeatureProvider.init()
//		executeBehaviorTree(bt, null);
		//System.out.println("execute bt");
		
		/*getSelf().getFeature(IExecutionFeature.class).scheduleStep(() -> 
		{
			executeBehaviorTree(bt, null);
			System.out.println("execute bt");
			return IFuture.DONE;
		}).catchEx(e -> getSelf().handleException(e));*/
	}
	
	@Override
	public void cleanup()
	{
		bt.abort(AbortMode.SUBTREE, NodeState.FAILED, context);
	}
	
	protected void initRulesystem()
	{
		Collection<Node<IComponent>> nodes = new ArrayList<>();
		bt.collectNodes(nodes);
		
		nodes.stream().forEach(node ->
		{
			/*if(node instanceof ActionNode<IComponent>)
			{
				ActionNode<IComponent> an = (ActionNode<IComponent>)node;
				
				UserBaseAction<IComponent, ? extends IFuture<NodeState>> action = an.getAction();
				
				BiFunction<Event, IComponent, ? extends IFuture<NodeState>> action2 = action.getAction();
				
				//if(!action2 instanceof DecouplingAction)
				
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
							});* /
							
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
			}*/
			
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
	
	public Node<IComponent> getBehaviorTree() 
	{
		return bt;
	}
}
