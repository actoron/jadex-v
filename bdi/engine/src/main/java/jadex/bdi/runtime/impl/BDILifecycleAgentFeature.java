package jadex.bdi.runtime.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jadex.bdi.model.IBDIClassGenerator;
import jadex.bdi.model.IBDIModel;
import jadex.bdi.model.MBelief;
import jadex.bdi.model.MCondition;
import jadex.bdi.model.MElement;
import jadex.bdi.model.MGoal;
import jadex.bdi.model.MParameter;
import jadex.bdi.model.MPlan;
import jadex.bdi.model.MTrigger;
import jadex.bdi.model.MParameter.EvaluationMode;
import jadex.bdi.runtime.ChangeEvent;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IDeliberationStrategy;
import jadex.bdi.runtime.impl.APL.CandidateInfoMPlan;
import jadex.bdi.runtime.impl.APL.MPlanInfo;
import jadex.bdi.runtime.impl.BDIAgentFeature.GoalsExistCondition;
import jadex.bdi.runtime.impl.BDIAgentFeature.LifecycleStateCondition;
import jadex.bdi.runtime.impl.BDIAgentFeature.NotInShutdownCondition;
import jadex.bdi.runtime.impl.BDIAgentFeature.PlansExistCondition;
import jadex.bdi.runtime.impl.RParameterElement.RParameter;
import jadex.bdi.runtime.impl.RParameterElement.RParameterSet;
import jadex.common.MethodInfo;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.common.UnparsedExpression;
import jadex.execution.IExecutionFeature;
import jadex.future.CollectionResultListener;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.javaparser.SJavaParser;
import jadex.micro.MicroAgent;
import jadex.micro.impl.MicroAgentFeature;
import jadex.model.IModelFeature;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IAction;
import jadex.rules.eca.ICondition;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.IRule;
import jadex.rules.eca.MethodCondition;
import jadex.rules.eca.Rule;
import jadex.rules.eca.RuleSystem;
import jadex.rules.eca.annotations.CombinedCondition;

/**
 *  Feature that ensures the agent created(), body() and killed() are called on the pojo. 
 */
public class BDILifecycleAgentFeature extends MicroAgentFeature implements IInternalBDILifecycleFeature
{
	/** Is the agent inited and allowed to execute rules? */
	protected boolean inited;
	
	/** Is the agent in shutdown?. */
	protected boolean shutdown;
	
	/**
	 *  Instantiate the feature.
	 */
	protected BDILifecycleAgentFeature(MicroAgent self)
	{
		super(self);
	}

	/**
	 *  Execute the functional body of the agent.
	 *  Is only called once.
	 */
	@Override
	public IFuture<Void> onStart()
	{
		IInternalBDIAgentFeature bdif = IInternalBDIAgentFeature.get();
		bdif.init();
		createStartBehavior().startBehavior(bdif.getBDIModel(), bdif.getRuleSystem(), bdif.getCapability());
		return super.onStart();
	}
	
	/**
	 *  Create the start behavior.
	 */
	protected StartBehavior createStartBehavior()
	{
		return new StartBehavior();
	}
	
	/**
	 *  Create the end behavior.
	 */
	protected EndBehavior createEndBehavior()
	{
		return new EndBehavior();
	}
	
	@Override
	public IFuture<Void>	onEnd()
	{
		setShutdown(true);
		IInternalBDIAgentFeature bdif = IInternalBDIAgentFeature.get();
		createEndBehavior().startEndBehavior(bdif.getBDIModel(), bdif.getRuleSystem(), bdif.getCapability()).get();
		super.onEnd().get();
		bdif.terminate();
		return IFuture.DONE;
	}
	
	/**
	 *  Execute a goal method.
	 */
	protected static boolean executeGoalMethod(Method m, RProcessableElement goal, IEvent event)
	{
		return invokeBooleanMethod(goal.getPojoElement(), m, goal.getModelElement(), event, null);
	}
	
	/**
	 *  Assemble fitting parameters from context and invoke a boolean method. 
	 */
	public static boolean	invokeBooleanMethod(Object pojo, Method m, MElement modelelement, IEvent event, RPlan rplan)
	{
		try
		{
			SAccess.setAccessible(m, true);
			
			@SuppressWarnings("rawtypes")
			Object[] vals = BDIAgentFeature.getInjectionValues(m.getParameterTypes(), m.getParameterAnnotations(),
				modelelement, event!=null ? new ChangeEvent(event) : null, rplan, null);
			if(vals==null)
				System.out.println("Invalid parameter assignment");
			Boolean val = (Boolean)m.invoke(pojo, vals);
			return val;
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Get the inited.
	 *  @return The inited.
	 */
	public boolean isInited()
	{
		return inited;
	}

	/**
	 *  The inited to set.
	 *  @param inited The inited to set
	 */
	public void setInited(boolean inited)
	{
		this.inited = inited;
	}
	

	/**
	 *  Get the shutdown. 
	 *  @return The shutdown
	 */
	public boolean isShutdown()
	{
		return shutdown;
	}

	/**
	 *  Set the shutdown.
	 *  @param shutdown The shutdown to set
	 */
	public void setShutdown(boolean shutdown)
	{
		this.shutdown = shutdown;
	}
	
	// for xml

	/**
	 *  Evaluate the condition.
	 *  @return
	 */
	public static boolean evaluateCondition(MCondition cond, MElement owner, Map<String, Object> vals)
	{
		// TODO BDIX only?
		throw new UnsupportedOperationException();
//		boolean ret = false;
//		
//		UnparsedExpression uexp = cond.getExpression();
//		try
//		{
//			Object res = SJavaParser.getParsedValue(uexp, agent.getModel().getAllImports(), CapabilityWrapper.getFetcher(agent, uexp.getLanguage(), vals), agent.getClassLoader());
//			if(res instanceof Boolean)
//			{
//				ret = ((Boolean)res).booleanValue();
//			}
//			else
//			{
//				agent.getLogger().warning("Condition does not evaluate to boolean: "+uexp.getValue());
//			}
//		}
//		catch(Exception e)
//		{
//			agent.getLogger().warning("Condition evaluation produced exception: "+uexp.getValue()+", "+e);
//		}
//		
//		return ret;
	}
	
	/**
	 *  Condition that tests if an expression evalutes to true.
	 */
	public static class EvaluateExpressionCondition implements ICondition
	{
		protected MCondition cond;
		protected MElement owner;
		protected Map<String, Object> vals;
		
		public EvaluateExpressionCondition(MCondition cond, MElement owner, Map<String, Object> vals)
		{
			this.cond = cond;
			this.owner = owner;
			this.vals = vals;
		}
		
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
//			vals.put("$event", event);
			boolean res = evaluateCondition(cond, owner, vals);
			return new Future<Tuple2<Boolean,Object>>(res? ICondition.TRUE: ICondition.FALSE);
		}
	}
	
	/**
	 *  Extracted start behavior. 
	 */
	public static class LifecycleBehavior
	{
		/**
		 *  Get the capability object (only for pojo).
		 */
		public Object getCapabilityObject(String name)
		{
			IBDIAgentFeature bdif = IInternalBDIAgentFeature.get();
			return ((BDIAgentFeature)bdif).getCapabilityObject(name);
		}
		
		/**
		 *  Dispatch a top level goal.
		 */
		public IFuture<Object> dispatchTopLevelGoal(Object goal)
		{
			IBDIAgentFeature bdif = IInternalBDIAgentFeature.get();
			return bdif.dispatchTopLevelGoal(goal);
		}
		
//		/**
//		 *  Dispatch a message event.
//		 */
//		public IFuture<Void> sendMessageEvent(IMessageEvent message)
//		{
//			IMessageFeature mf = component.getFeature(IMessageFeature.class);
//			return mf.sendMessage(message.getMessage());
//		}
		
//		/**
//		 *  Dispatch an internal event.
//		 */
//		public IFuture<Void> dispatchInternalEvent(IInternalEvent event)
//		{
//			// Pojo bdi does not support internal events
//			throw new UnsupportedOperationException();
//		}
//		
//		/**
//		 *  Dispatch the configuration plans.
//		 */
//		protected void	dispatchConfigPlans(List<MConfigParameterElement> cplans, IBDIModel bdimodel)
//		{
//			if(cplans!=null && cplans.size()>0)
//			{
//				for(MConfigParameterElement cplan: cplans)
//				{
//					MPlan mplan = bdimodel.getCapability().getPlan(cplan.getRef());
//					// todo: allow Java plan constructor calls
//	//				Object val = SJavaParser.parseExpression(uexp, model.getModelInfo().getAllImports(), getClassLoader());
//					
//					// todo: bindings in config elems
//					
//					List<Map<String, Object>> bindings = APL.calculateBindingElements(mplan, null);
//					
//					if(bindings!=null)
//					{
//						for(Map<String, Object> binding: bindings)
//						{
//							RPlan rplan = RPlan.createRPlan(mplan, new CandidateInfoMPlan(new MPlanInfo(mplan, binding), null), null, null, cplan);
//							rplan.executePlan();
//						}
//					}
//					// No binding: generate one candidate.
//					else
//					{
//						RPlan rplan = RPlan.createRPlan(mplan, new CandidateInfoMPlan(new MPlanInfo(mplan, null), null), null, null, cplan);
//						rplan.executePlan();
//					}
//				}
//			}
//		}
		
//		/**
//		 *  Dispatch the configuration goals.
//		 */
//		protected IFuture<Void> dispatchConfigGoals(List<MConfigParameterElement> cgoals, IBDIModel bdimodel)
//		{
//			Future<Void> ret = new Future<Void>();
//			if(cgoals!=null && cgoals.size()>0)
//			{
//				FutureBarrier<Object> barrier = new FutureBarrier<Object>();
//				
//				for(MConfigParameterElement cgoal: cgoals)
//				{
//					MGoal mgoal = null;
//					Class<?> gcl = null;
//					Object goal = null;
//					
//					// try to fetch via name
//					mgoal = bdimodel.getCapability().getGoal(cgoal.getRef());
//					if(mgoal==null && cgoal.getRef().indexOf(".")==-1)
//					{
//						// try with package
//						mgoal = bdimodel.getCapability().getGoal(IInternalBDIAgentFeature.get().getBDIModel().getModelInfo().getPackage()+"."+cgoal.getRef());
//					}
//					
//					if(mgoal!=null)
//					{
//						gcl = mgoal.getTargetClass(IInternalBDIAgentFeature.get().getClassLoader());
//					}
//					// if not found, try expression
//					else
//					{
//						Object o = SJavaParser.parseExpression(cgoal.getRef(), IInternalBDIAgentFeature.get().getBDIModel().getModelInfo().getAllImports(), IInternalBDIAgentFeature.get().getClassLoader())
//							.getValue(CapabilityWrapper.getFetcher(cgoal.getCapabilityName()));
//						if(o instanceof Class)
//						{
//							gcl = (Class<?>)o;
//						}
//						else
//						{
//							goal = o;
//							gcl = o.getClass();
//						}
//						mgoal = bdimodel.getCapability().getGoal(gcl.getName());
//					}
//		
////					// Create goal if expression available
////					if(uexp.getName()!=null && uexp.getValue().length()>0)
////					{
////						Object o = SJavaParser.parseExpression(uexp, component.getModel().getAllImports(), component.getClassLoader()).getValue(component.getFetcher());
////						if(o instanceof Class)
////						{
////							gcl = (Class<?>)o;
////						}
////						else
////						{
////							goal = o;
////							gcl = o.getClass();
////						}
////					}
////					
////					if(gcl==null && uexp.getClazz()!=null)
////					{
////						gcl = uexp.getClazz().getType(component.getClassLoader(), component.getModel().getAllImports());
////					}
////					if(gcl==null)
////					{
////						// try to fetch via name
////						mgoal = bdimodel.getCapability().getGoal(uexp.getName());
////						if(mgoal==null && uexp.getName().indexOf(".")==-1)
////						{
////							// try with package
////							mgoal = bdimodel.getCapability().getGoal(component.getModel().getPackage()+"."+uexp.getName());
////						}
////						if(mgoal!=null)
////						{
////							gcl = mgoal.getTargetClass(component.getClassLoader());
////						}
////					}						
////					if(mgoal==null)
////					{
////						mgoal = bdimodel.getCapability().getGoal(gcl.getName());
////					}
//					
//					// Create goal instance
//					if(goal==null && gcl!=null)
//					{
//						try
//						{
//							MicroAgentFeature maf = MicroAgentFeature.get();
//							Object agent = maf.getSelf().getPojo();
//							Class<?> agcl = agent.getClass();
//							Constructor<?>[] cons = gcl.getDeclaredConstructors();
//							for(Constructor<?> c: cons)
//							{
//								Class<?>[] params = c.getParameterTypes();
//								if(params.length==0)
//								{
//									// perfect found empty con
//									goal = gcl.getConstructor().newInstance();
//									break;
//								}
//								else if(params.length==1 && params[0].equals(agcl))
//								{
//									// found (first level) inner class constructor
//									goal = c.newInstance(new Object[]{agent});
//									break;
//								}
//							}
//						}
//						catch(RuntimeException e)
//						{
//							throw e;
//						}
//						catch(Exception e)
//						{
//							throw new RuntimeException(e);
//						}
//					}
//					
//					if(mgoal==null || (goal==null && gcl!=null))
//					{
//						throw new RuntimeException("Could not create goal: "+cgoal);
//					}
//					
//					List<Map<String, Object>> bindings = APL.calculateBindingElements(mgoal, null);
//					
//					if(goal==null)
//					{
//						// XML only
//						if(bindings!=null)
//						{
//							for(Map<String, Object> binding: bindings)
//							{
//								RGoal rgoal = new RGoal(mgoal, null, null, binding, cgoal, null);
//								barrier.addFuture(dispatchTopLevelGoal(rgoal));//.addResultListener(goallis);
//							}
//						}
//						// No binding: generate one candidate.
//						else
//						{
//							RGoal rgoal = new RGoal(mgoal, goal, null, null, cgoal, null);
//							barrier.addFuture(dispatchTopLevelGoal(rgoal));//.addResultListener(goallis);
//						}
//					}
//					else
//					{
//						// Pojo only
//						barrier.addFuture(dispatchTopLevelGoal(goal));//.addResultListener(goallis);								
//					}
//				}
//				
//				// wait for all goals being finished
//				barrier.waitForIgnoreFailures(new ICommand<Exception>()
//				{
//					@Override
//					public void execute(Exception e)
//					{
//						if(e instanceof GoalDroppedException)
//						{
//							System.err.println("Config goal has been dropped: "+e);
//						}
//						else
//						{
//							System.err.println("Failure during config goal processing: "+SUtil.getExceptionStacktrace(e));							
//						}
//					}
//				}).addResultListener(new DelegationResultListener<Void>(ret));
//			}
//			else
//			{
//				ret.setResult(null);
//			}
//			
//			return ret;
//		}
	}
	
	/**
	 *  Extracted start behavior. 
	 */
	public static class StartBehavior extends LifecycleBehavior
	{
		/**
		 *  Start the component behavior.
		 */
		public void startBehavior(final IBDIModel bdimodel, final RuleSystem rulesystem, final RCapability rcapa)
		{
//			super.startBehavior();
			
//			final Object agent = microagent instanceof PojoBDIAgent? ((PojoBDIAgent)microagent).getPojoAgent(): microagent;
					
//			final IBDIAgentFeature bdif = component.getComponentFeature(IBDIAgentFeature.class);
//			final IInternalBDIAgentFeature ibdif = (IInternalBDIAgentFeature)bdif; 
//			final IBDIModel bdimodel = ibdif.getBDIModel();
			
			final IResultListener<Object> goallis = new IResultListener<Object>()
			{
				public void resultAvailable(Object result)
				{
//					System.err.println("Goal succeeded: "+result);
				}
				
				public void exceptionOccurred(Exception exception)
				{
					System.err.println("Goal failed: "+exception);
				}
			};
			
//			// Init bdi configuration
//			String confname = component.getConfiguration();
//			if(confname!=null)
//			{
//				MConfiguration mconf = bdimodel.getCapability().getConfiguration(confname);
//				
//				if(mconf!=null)
//				{
//					// only for pojo agents / xml is inited in beliefbase init
//					if(bdimodel instanceof BDIModel)
//					{
//						// Set initial belief values
//						List<MConfigBeliefElement> ibels = mconf.getInitialBeliefs();
//						if(ibels!=null)
//						{
//							for(MConfigBeliefElement ibel: ibels)
//							{
//								try
//								{
//									UnparsedExpression	fact	= ibel.getFacts().get(0);	// pojo initial beliefs are @NameValue, thus exactly one fact.
//									MBelief mbel = bdimodel.getCapability().getBelief(ibel.getName());
//									Object val = SJavaParser.parseExpression(fact, component.getModel().getAllImports(), component.getClassLoader()).getValue(CapabilityWrapper.getFetcher(component, fact.getLanguage()));
//									mbel.setValue(component, val);
//								}
//								catch(RuntimeException e)
//								{
//									throw e;
//								}
//								catch(Exception e)
//								{
//									throw new RuntimeException(e);
//								}
//							}
//						}
//					}
//					
//					// Create initial plans (create plans before other elements as they might want to react to these)
//					List<MConfigParameterElement> iplans = mconf.getInitialPlans();
//					dispatchConfigPlans(component, iplans, bdimodel);
//					
//					// Create initial goals
//					List<MConfigParameterElement> igoals = mconf.getInitialGoals();
//					dispatchConfigGoals(component, igoals, bdimodel);
//					
//					// Create initial events
//					List<MConfigParameterElement> ievents = mconf.getInitialEvents();
//					dispatchConfigEvents(component, ievents, bdimodel);
//				}
//			}
			
			// Observe dynamic beliefs
			List<MBelief> beliefs = bdimodel.getCapability().getBeliefs();
			
			for(final MBelief mbel: beliefs)
			{
				List<EventType> events = mbel.getEvents();
				
//				Object cap = null;
//				if(component.getComponentFeature0(IPojoComponentFeature.class)!=null)
//				{
//					Object agent = component.getComponentFeature(IPojoComponentFeature.class).getPojoAgent();
//					Object ocapa = agent;
//					int	i	= mbel.getName().indexOf(MElement.CAPABILITY_SEPARATOR);
//					if(i!=-1)
//					{
//						ocapa	= ((BDIAgentFeature)bdif).getCapabilityObject(mbel.getName().substring(0, mbel.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR)));
//					}
//					cap	= ocapa;
//				}
//				final Object fcapa = cap;
				
				String name = null;
				Object capa = null;
//				if(component.getFeature0(IPojoComponentFeature.class)!=null)
				{
					int	i	= mbel.getName().indexOf(MElement.CAPABILITY_SEPARATOR);
					if(i!=-1)
					{
						capa	= getCapabilityObject(mbel.getName().substring(0, mbel.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR)));
						name	= mbel.getName().substring(mbel.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR)+1); 
					}
					else
					{
						Object agent = MicroAgentFeature.get().getSelf().getPojo();
						capa	= agent;
						name	= mbel.getName();
					}
				}
				final String fname = name;
				final Object fcapa = capa;
				
				// Automatic reevaluation if belief depends on other beliefs
				if(!events.isEmpty() && mbel.getEvaluationMode().equals(EvaluationMode.PUSH))
				{
					Rule<Void> rule = new Rule<Void>(mbel.getName()+"_belief_update", 
						ICondition.TRUE_CONDITION, new IAction<Void>()
					{
						Object oldval = null;
						
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
//							System.out.println("belief update: "+event);
							// Invoke dynamic update method if field belief
							if(mbel.isFieldBelief())
							{
								try
								{
									Method um = fcapa.getClass().getMethod(IBDIClassGenerator.DYNAMIC_BELIEF_UPDATEMETHOD_PREFIX+SUtil.firstToUpperCase(mbel.getName()), new Class[0]);
									um.invoke(fcapa, new Object[0]);
								}
								catch(Exception e)
								{
									e.printStackTrace();
								}
							}
							// Otherwise just call getValue and throw event
							else if(fcapa!=null) // if is pojo 
							{
								Object value = mbel.getValue();
								// todo: save old value?!
								BDIAgentFeature.createChangeEvent(value, oldval, null, mbel.getName());
								oldval = value;
							}
//							else // xml belief push mode
//							{
//								// reevaluate the belief on change events
//								Object value = SJavaParser.parseExpression(mbel.getDefaultFact(), 
//									IInternalBDIAgentFeature.get().getBDIModel().getModelInfo().getAllImports(), IInternalBDIAgentFeature.get().getClassLoader()).getValue(CapabilityWrapper.getFetcher(mbel.getDefaultFact().getLanguage()));
//								// save the value
//								mbel.setValue(value);
////								oldval = value;	// not needed for xml
//							}
							return IFuture.DONE;
						}
					});
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
				}
				
				if(mbel.getUpdaterateValue()>0)
				{
					Consumer<Void>	update	= new Consumer<>()
					{
						Consumer<Void>	update	= this;
						Object oldval = null;
						
						public void accept(Void v)
						{
//							System.out.println("belief update "+component+", "+mbel);
							try
							{
								// Invoke dynamic update method if field belief
								if(mbel.isFieldBelief())
								{
									Method um = fcapa.getClass().getMethod(IBDIClassGenerator.DYNAMIC_BELIEF_UPDATEMETHOD_PREFIX+SUtil.firstToUpperCase(fname), new Class[0]);
									um.invoke(fcapa, new Object[0]);
								}
								// Otherwise just call getValue and throw event
								else if(fcapa!=null)
								{
									Object value = mbel.getValue(fcapa, IInternalBDIAgentFeature.get().getClassLoader());
									BDIAgentFeature.createChangeEvent(value, oldval, null, mbel.getName());
									oldval = value;
								}
//								else // xml belief updaterate
//								{
//									// reevaluate the belief on change events
//									Object value = SJavaParser.parseExpression(mbel.getDefaultFact(), 
//										component.getModel().getAllImports(), component.getClassLoader()).getValue(CapabilityWrapper.getFetcher(component, mbel.getDefaultFact().getLanguage()));
//									// save the value 
//									// change event is automatically thrown
//									mbel.setValue(component, value);
//									oldval = value;
//								}
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
							
							IExecutionFeature.get().waitForDelay(mbel.getUpdaterateValue()).then(update);
						}
					
						@Override
						public String toString()
						{
							return "updateBelief("+mbel.getName()+")";//+"@"+component.getId()+")";
						}
						
					};
					// Evaluate at time 0, updaterate*1, updaterate*2, ...
					update.accept(null);
				}
			}
			
			// Observe dynamic parameters of goals
			// todo: other parameter elements?!
			List<MGoal> mgoals = bdimodel.getCapability().getGoals();
			
			for(final MGoal mgoal: mgoals)
			{
				List<MParameter> mparams = mgoal.getParameters();
				
				if(mparams!=null)
				{
					for(final MParameter mparam: mparams)
					{
						if(mparam.getEvaluationMode().equals(EvaluationMode.PUSH))
						{
							List<EventType> events = mparam.getEvents();
						
							// Automatic reevaluation if belief depends on other beliefs
							if(!events.isEmpty())
							{
								Rule<Void> rule = new Rule<Void>(mgoal.getName()+"_"+mparam.getName()+"_parameter_update", 
									ICondition.TRUE_CONDITION, new IAction<Void>()
								{
									// todo: oldval
			//						Object oldval = null;
									
									public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
									{
//										System.out.println("parameter update: "+event);
										
										RCapability capa = IInternalBDIAgentFeature.get().getCapability();
										for(RGoal goal: SUtil.notNull(capa.getGoals(mgoal)))
										{
											if(!mparam.isMulti(IInternalBDIAgentFeature.get().getClassLoader()))
											{
												((RParameter)goal.getParameter(mparam.getName())).updateDynamicValue();
											}
											else
											{
												((RParameterSet)goal.getParameterSet(mparam.getName())).updateDynamicValues();
											}
										}
										
										return IFuture.DONE;
									}
								});
								
								rule.setEvents(events);
								rulesystem.getRulebase().addRule(rule);
							}
							
							if(mparam.getUpdaterateValue()>0)
							{
								Consumer<Void>	update	= new Consumer<>()
								{
									Consumer<Void>	update	= this;
									
									public void accept(Void v)
									{
										try
										{
											System.out.println("parameter updaterate: "+mparam.getUpdaterateValue());
											
											RCapability capa = IInternalBDIAgentFeature.get().getCapability();
											for(RGoal goal: SUtil.notNull(capa.getGoals(mgoal)))
											{
												if(!mparam.isMulti(IInternalBDIAgentFeature.get().getClassLoader()))
												{
													((RParameter)goal.getParameter(mparam.getName())).updateDynamicValue();
												}
												else
												{
													((RParameterSet)goal.getParameterSet(mparam.getName())).updateDynamicValues();
												}
											}
										}
										catch(Exception e)
										{
											e.printStackTrace();
										}
										
										IExecutionFeature.get().waitForDelay(mparam.getUpdaterateValue()).then(update);
									}
								};
								// Evaluate at time 0, updaterate*1, updaterate*2, ...
								update.accept(null);
							}
						}
					}
				}
			}
			
			// Observe goal types
			List<MGoal> goals = bdimodel.getCapability().getGoals();
			for(final MGoal mgoal: goals)
			{
//				todo: explicit bdi creation rule
//				rulesystem.observeObject(goals.get(i).getTargetClass(getClassLoader()));
			
//				boolean fin = false;
				
				final Class<?> gcl = mgoal.getTargetClass(IInternalBDIAgentFeature.get().getClassLoader());
//				boolean declarative = false;
//				boolean maintain = false;
				
				List<MCondition> conds = mgoal.getConditions(MGoal.CONDITION_CREATION);
				if(conds!=null)
				{
					for(MCondition cond: conds)
					{
						if(cond.getConstructorTarget()!=null)
						{
							final Constructor<?> c = cond.getConstructorTarget().getConstructor(IInternalBDIAgentFeature.get().getClassLoader());
							
							Rule<Void> rule = new Rule<Void>(mgoal.getName()+"_goal_create", 
								new NotInShutdownCondition(), new IAction<Void>()
							{
								@SuppressWarnings({"rawtypes"})
								public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
								{
		//							System.out.println("create: "+context);
									
									Object pojogoal = null;
									try
									{
										boolean ok = true;
										Class<?>[] ptypes = c.getParameterTypes();
										Object[] pvals = new Object[ptypes.length];
										
//										Annotation[][] anns = c.getParameterAnnotations();
//										int skip = ptypes.length - anns.length;
										
										for(int i=0; i<ptypes.length; i++)
										{
											Object agent = MicroAgentFeature.get().getSelf().getPojo();
											Object	o	= event.getContent();
											if(o!=null && SReflect.isSupertype(ptypes[i], o.getClass()))
											{
												pvals[i] = o;
											}
											else if(o instanceof ChangeInfo<?> && ((ChangeInfo)o).getValue()!=null && SReflect.isSupertype(ptypes[i], ((ChangeInfo)o).getValue().getClass()))
											{
												pvals[i] = ((ChangeInfo)o).getValue();
											}
											else if(SReflect.isSupertype(agent.getClass(), ptypes[i]))
											{
												pvals[i] = agent;
											}
											
											// ignore implicit parameters of inner class constructor
											//TODO ???
//											if(pvals[i]==null && i>=skip)
//											{
//												for(int j=0; anns!=null && j<anns[i-skip].length; j++)
//												{
//													if(anns[i-skip][j] instanceof CheckNotNull)
//													{
//														ok = false;
//														break;
//													}
//												}
//											}
										}
										
										if(ok)
										{
											SAccess.setAccessible(c, true);
											pojogoal = c.newInstance(pvals);
										}
									}
									catch(RuntimeException e)
									{
										throw e;
									}
									catch(Exception e)
									{
										throw new RuntimeException(e);
									}
									
									if(pojogoal!=null && !rcapa.containsGoal(pojogoal))
									{
										dispatchTopLevelGoal(pojogoal).addResultListener(goallis);
									}
//									else
//									{
//										System.out.println("new goal not adopted, already contained: "+pojogoal);
//									}
								
									return IFuture.DONE;
								}
							});
							rule.setEvents(cond.getEvents());
							rulesystem.getRulebase().addRule(rule);
						}
						else if(cond.getMethodTarget()!=null)
						{
							final Method m = cond.getMethodTarget().getMethod(IInternalBDIAgentFeature.get().getClassLoader());
							
							Rule<Void> rule = new Rule<Void>(mgoal.getName()+"_goal_create", 
								new CombinedCondition(new ICondition[]{new NotInShutdownCondition(), new MethodCondition(null, m)
							{
								protected Object invokeMethod(IEvent event) throws Exception
								{
									SAccess.setAccessible(m, true);
									Object[] pvals = BDIAgentFeature.getInjectionValues(m.getParameterTypes(), m.getParameterAnnotations(),
										mgoal, new ChangeEvent<Object>(event), null, null);
									return pvals!=null? m.invoke(null, pvals): null;
								}
							}}), new IAction<Void>()
							{
								public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
								{
			//						System.out.println("create: "+context);
									
									if(condresult!=null)
									{
										if(SReflect.isIterable(condresult))
										{
											for(Iterator<Object> it = SReflect.getIterator(condresult); it.hasNext(); )
											{
												Object pojogoal = it.next();
												dispatchTopLevelGoal(pojogoal).addResultListener(goallis);
											}
										}
										else
										{
											dispatchTopLevelGoal(condresult).addResultListener(goallis);
										}
									}
									else
									{
										Constructor<?>[] cons = gcl.getConstructors();
										Object pojogoal = null;
										boolean ok = false;
										for(Constructor<?> c: cons)
										{
											try
											{
												Object[] vals = BDIAgentFeature.getInjectionValues(c.getParameterTypes(), c.getParameterAnnotations(),
													mgoal, new ChangeEvent<Object>(event), null, null);
												if(vals!=null)
												{
													pojogoal = c.newInstance(vals);
													dispatchTopLevelGoal(pojogoal).addResultListener(goallis);
													break;
												}
												else
												{
													ok = true;
												}
											}
											catch(Exception e)
											{
											}
										}
										if(pojogoal==null && !ok)
											throw new RuntimeException("Unknown how to create goal: "+gcl);
									}
									return IFuture.DONE;
								}
							});
							rule.setEvents(cond.getEvents());
							rulesystem.getRulebase().addRule(rule);
						}
						else
						{
							Rule<Void> rule = new Rule<Void>(mgoal.getName()+"_goal_create", 
								new CombinedCondition(new ICondition[]{new NotInShutdownCondition(), new EvaluateExpressionCondition(cond, mgoal, null)}), new IAction<Void>()
							{
								public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
								{
			//						System.out.println("create: "+create);
									
									List<Map<String, Object>> bindings = APL.calculateBindingElements(mgoal, null);
									
									if(bindings!=null)
									{
										for(Map<String, Object> binding: bindings)
										{
											RGoal rgoal = new RGoal(mgoal, null, null, binding, null, null);
											dispatchTopLevelGoal(rgoal).addResultListener(goallis);
										}
									}
									// No binding: generate one candidate.
									else
									{
										RGoal rgoal = new RGoal(mgoal, null, null, null, null, null);
										dispatchTopLevelGoal(rgoal).addResultListener(goallis);
									}
									
									return IFuture.DONE;
								}
							});
							
							rule.setEvents(cond.getEvents());
							rulesystem.getRulebase().addRule(rule);
						}
					}
				}
				
				conds = mgoal.getConditions(MGoal.CONDITION_DROP);
				if(conds!=null)
				{
					for(final MCondition cond: conds)
					{
						final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(IInternalBDIAgentFeature.get().getClassLoader());
						
						Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_drop", 
							new GoalsExistCondition(mgoal, rcapa), new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
								for(final RGoal goal: rcapa.getGoals(mgoal))
								{
									if(!RGoal.GoalLifecycleState.DROPPING.equals(goal.getLifecycleState())
										 && !RGoal.GoalLifecycleState.DROPPED.equals(goal.getLifecycleState()))
									{
										if(m!=null)
										{
											if(executeGoalMethod(m, goal, event))
											{
//												System.out.println("Goal dropping triggered: "+goal);
				//								rgoal.setLifecycleState(BDIAgent.this, rgoal.GOALLIFECYCLESTATE_DROPPING);
												if(!goal.isFinished())
												{
													goal.setException(new GoalDroppedException("drop condition: "+m.getName()));
//															{
//																public void printStackTrace() 
//																{
//																	super.printStackTrace();
//																}
//															});
													goal.setProcessingState(RGoal.GoalProcessingState.FAILED);
												}
											}
										}
										else
										{
											if(evaluateCondition(cond, mgoal, SUtil.createHashMap(new String[]{goal.getFetcherName()}, new Object[]{goal})))
											{
												if(!goal.isFinished())
												{
													goal.setException(new GoalDroppedException("drop condition: "+goal));
													goal.setProcessingState(RGoal.GoalProcessingState.FAILED);
												}
											}
										}
									}
								}
								
								return IFuture.DONE;
							}
						});
						List<EventType> events = new ArrayList<EventType>(cond.getEvents());
						events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
						rule.setEvents(events);
						rulesystem.getRulebase().addRule(rule);
//							rule.setEvents(cond.getEvents());
//							rulesystem.getRulebase().addRule(rule);
					}
				}
				
				conds = mgoal.getConditions(MGoal.CONDITION_CONTEXT);
				if(conds!=null)
				{
					for(final MCondition cond: conds)
					{
						final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(IInternalBDIAgentFeature.get().getClassLoader());
						
						Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_suspend", 
							new GoalsExistCondition(mgoal, rcapa), new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
								for(final RGoal goal: rcapa.getGoals(mgoal))
								{
									if(!RGoal.GoalLifecycleState.SUSPENDED.equals(goal.getLifecycleState())
									  && !RGoal.GoalLifecycleState.DROPPING.equals(goal.getLifecycleState())
									  && !RGoal.GoalLifecycleState.DROPPED.equals(goal.getLifecycleState()))
									{	
										if(m!=null)
										{
											if(!executeGoalMethod(m, goal, event))
											{
//												if(goal.getMGoal().getName().indexOf("AchieveCleanup")!=-1)
//													System.out.println("Goal suspended: "+goal);
												goal.setLifecycleState(RGoal.GoalLifecycleState.SUSPENDED);
												goal.setState(RProcessableElement.State.INITIAL);
											}
										}
										else
										{
											if(!evaluateCondition(cond, mgoal, SUtil.createHashMap(new String[]{goal.getFetcherName()}, new Object[]{goal})))
											{
												goal.setLifecycleState(RGoal.GoalLifecycleState.SUSPENDED);
												goal.setState(RProcessableElement.State.INITIAL);
											}
										}
									}
								}
								return IFuture.DONE;
							}
						});
						List<EventType> events = new ArrayList<EventType>(cond.getEvents());
						events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
						rule.setEvents(events);
						rulesystem.getRulebase().addRule(rule);
						
//							rule.setEvents(cond.getEvents());
//							rulesystem.getRulebase().addRule(rule);
						
						rule = new Rule<Void>(mgoal.getName()+"_goal_option", 
							new GoalsExistCondition(mgoal, rcapa), new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
								for(final RGoal goal: rcapa.getGoals(mgoal))
								{
									if(RGoal.GoalLifecycleState.SUSPENDED.equals(goal.getLifecycleState()))
									{	
										if(m!=null)
										{
											if(executeGoalMethod(m, goal, event))
											{
//												if(goal.getMGoal().getName().indexOf("AchieveCleanup")!=-1)
//												System.out.println("Goal made option: "+goal);
												goal.setLifecycleState(RGoal.GoalLifecycleState.OPTION);
//												setState(ia, PROCESSABLEELEMENT_INITIAL);
											}
										}
										else
										{
											if(evaluateCondition(cond, mgoal, SUtil.createHashMap(new String[]{goal.getFetcherName()}, new Object[]{goal})))
											{
												goal.setLifecycleState(RGoal.GoalLifecycleState.OPTION);
											}
										}
									}
								}
								
								return IFuture.DONE;
							}
						});
						rule.setEvents(events);
						rulesystem.getRulebase().addRule(rule);
						
//							rule.setEvents(cond.getEvents());
//							rulesystem.getRulebase().addRule(rule);
					}
				}
				
				conds = mgoal.getConditions(MGoal.CONDITION_TARGET);
				if(conds!=null)
				{
					for(final MCondition cond: conds)
					{
						final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(IInternalBDIAgentFeature.get().getClassLoader());
											
						Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_target", 
							new CombinedCondition(new ICondition[]{
								new GoalsExistCondition(mgoal, rcapa)
			//							, new LifecycleStateCondition(SUtil.createHashSet(new String[]
			//							{
			//								RGoal.GOALLIFECYCLESTATE_ACTIVE,
			//								RGoal.GOALLIFECYCLESTATE_ADOPTED,
			//								RGoal.GOALLIFECYCLESTATE_OPTION,
			//								RGoal.GOALLIFECYCLESTATE_SUSPENDED
			//							}))
							}),
							new IAction<Void>()
						{
							public IFuture<Void> execute(final IEvent event, final IRule<Void> rule, final Object context, Object condresult)
							{
//								if(mgoal.getName().indexOf("cleanup")!=-1)
//									System.out.println("target test");
								
								for(final RGoal goal: rcapa.getGoals(mgoal))
								{
									if(m!=null)
									{
										if(executeGoalMethod(m, goal, event))
										{
											if(!goal.isFinished())
											{
												goal.targetConditionTriggered(event, rule, context);
											}
										}
									}
									else
									{
										if(!goal.isFinished() && evaluateCondition(cond, mgoal, SUtil.createHashMap(new String[]{goal.getFetcherName()}, new Object[]{goal})))
										{
											goal.targetConditionTriggered(event, rule, context);
										}
									}
								}
							
								return IFuture.DONE;
							}
						});
						List<EventType> events = cond.getEvents()==null || cond.getEvents().size()==0? new ArrayList<EventType>(): new ArrayList<EventType>(cond.getEvents());
						events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
						rule.setEvents(events);
						rulesystem.getRulebase().addRule(rule);
					}
				}
				
				conds = mgoal.getConditions(MGoal.CONDITION_RECUR);
				if(conds!=null)
				{
					for(final MCondition cond: conds)
					{
						final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(IInternalBDIAgentFeature.get().getClassLoader());
											
						Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_recur",
							new GoalsExistCondition(mgoal, rcapa), new IAction<Void>()
		//						new CombinedCondition(new ICondition[]{
		//							new LifecycleStateCondition(GOALLIFECYCLESTATE_ACTIVE),
		//							new ProcessingStateCondition(GOALPROCESSINGSTATE_PAUSED),
		//							new MethodCondition(getPojoElement(), m),
		//						}), new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
								for(final RGoal goal: rcapa.getGoals(mgoal))
								{
									if(RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState())
										&& RGoal.GoalProcessingState.PAUSED.equals(goal.getProcessingState()))
									{	
										if(m!=null)
										{
											if(executeGoalMethod(m, goal, event))
											{
												goal.setTriedPlans(null);
												goal.setApplicablePlanList(null);
												goal.setProcessingState(RGoal.GoalProcessingState.INPROCESS);
											}
										}
										else
										{
											if(evaluateCondition(cond, mgoal, SUtil.createHashMap(new String[]{goal.getFetcherName()}, new Object[]{goal})))
											{
												goal.setTriedPlans(null);
												goal.setApplicablePlanList(null);
												goal.setProcessingState(RGoal.GoalProcessingState.INPROCESS);
											}
										}
									}
								}
								return IFuture.DONE;
							}
						});
						rule.setEvents(cond.getEvents());
						rulesystem.getRulebase().addRule(rule);
					}
				}
				
				conds = mgoal.getConditions(MGoal.CONDITION_MAINTAIN);
				if(conds!=null)
				{
					for(final MCondition cond: conds)
					{
						final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(IInternalBDIAgentFeature.get().getClassLoader());
						
						Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_maintain", 
							new GoalsExistCondition(mgoal, rcapa), new IAction<Void>()
		//						new CombinedCondition(new ICondition[]{
		//							new LifecycleStateCondition(GOALLIFECYCLESTATE_ACTIVE),
		//							new ProcessingStateCondition(GOALPROCESSINGSTATE_IDLE),
		//							new MethodCondition(getPojoElement(), mcond, true),
		//						}), new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
								for(final RGoal goal: rcapa.getGoals(mgoal))
								{
									if(RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState())
										&& RGoal.GoalProcessingState.IDLE.equals(goal.getProcessingState()))
									{	
										if(m!=null)
										{
											if(!executeGoalMethod(m, goal, event))
											{
//												System.out.println("Goal maintain triggered: "+goal);
//												System.out.println("state was: "+goal.getProcessingState());
												goal.setProcessingState(RGoal.GoalProcessingState.INPROCESS);
											}
										}
										else // xml expression
										{
											if(!evaluateCondition(cond, mgoal, SUtil.createHashMap(new String[]{goal.getFetcherName()}, new Object[]{goal})))
											{
												goal.setProcessingState(RGoal.GoalProcessingState.INPROCESS);
											}
										}
									}
								}
								return IFuture.DONE;
							}
						});
						List<EventType> events = new ArrayList<EventType>(cond.getEvents());
						events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
						rule.setEvents(events);
						rulesystem.getRulebase().addRule(rule);
						
						// if has no own target condition
						if(mgoal.getConditions(MGoal.CONDITION_TARGET)==null)
						{
							// if not has own target condition use the maintain cond
							rule = new Rule<Void>(mgoal.getName()+"_goal_target", 
								new GoalsExistCondition(mgoal, rcapa), new IAction<Void>()
		//							new MethodCondition(getPojoElement(), mcond), new IAction<Void>()
							{
								public IFuture<Void> execute(final IEvent event, final IRule<Void> rule, final Object context, Object condresult)
								{
									for(final RGoal goal: rcapa.getGoals(mgoal))
									{
										if(m!=null)
										{
											if(executeGoalMethod(m, goal, event))
											{
												goal.targetConditionTriggered(event, rule, context);
											}
										}
										else // xml expression
										{
											if(evaluateCondition(cond, mgoal, SUtil.createHashMap(new String[]{goal.getFetcherName()}, new Object[]{goal})))
											{
												goal.targetConditionTriggered(event, rule, context);
											}
										}
									}
									
									return IFuture.DONE;
								}
							});
							rule.setEvents(cond.getEvents());
							rulesystem.getRulebase().addRule(rule);
						}
					}
				}
			}
			
			// Observe plan types
			List<MPlan> mplans = bdimodel.getCapability().getPlans();
			for(int i=0; i<mplans.size(); i++)
			{
				final MPlan mplan = mplans.get(i);
				
				IAction<Void> createplan = new IAction<Void>()
				{
					public IFuture<Void> execute(final IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						// Create all binding plans
						List<ICandidateInfo> cands = APL.createMPlanCandidates(mplan, null);

						final CollectionResultListener<ICandidateInfo> lis = new CollectionResultListener<ICandidateInfo>(cands.size(), 
							new IResultListener<Collection<ICandidateInfo>>()
						{
							public void resultAvailable(final Collection<ICandidateInfo> result)
							{
								for(ICandidateInfo ci: result)
								{
//									System.out.println("Create plan 1: "+mplan);
									RPlan rplan = RPlan.createRPlan(mplan, new CandidateInfoMPlan(new MPlanInfo(mplan, null), null), new ChangeEvent<Object>(event), ((MPlanInfo)ci.getRawCandidate()).getBinding(), null);
//									System.out.println("Create plan 2: "+mplan);
									rplan.executePlan();
								}
							}
							
							public void exceptionOccurred(Exception exception)
							{
							}
						});
						
						for(final ICandidateInfo cand: cands)
						{
							// check precondition
							if(APL.checkMPlan(cand, null))
							{
								lis.resultAvailable(cand);
							}
							else
							{
								lis.exceptionOccurred(null);
							}
						}
						
						return IFuture.DONE;
					}
				};
				
				MTrigger trigger = mplan.getTrigger();
				
				if(trigger!=null)
				{
					List<String> fas = trigger.getFactAddeds();
					if(fas!=null && fas.size()>0)
					{
						// todo: hmm turn off these too? new NotInShutdownCondition(component)
						Rule<Void> rule = new Rule<Void>("create_plan_factadded_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
						for(String fa: fas)
						{
							rule.addEvent(new EventType(new String[]{ChangeEvent.FACTADDED, fa}));
						}
						rulesystem.getRulebase().addRule(rule);
					}
		
					List<String> frs = trigger.getFactRemoveds();
					if(frs!=null && frs.size()>0)
					{
						Rule<Void> rule = new Rule<Void>("create_plan_factremoved_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
						for(String fr: frs)
						{
							rule.addEvent(new EventType(new String[]{ChangeEvent.FACTREMOVED, fr}));
						}
						rulesystem.getRulebase().addRule(rule);
					}
					
					List<String> fcs = trigger.getFactChangeds();
					if(fcs!=null && fcs.size()>0)
					{
						Rule<Void> rule = new Rule<Void>("create_plan_factchanged_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
						for(String fc: fcs)
						{
							rule.addEvent(new EventType(new String[]{ChangeEvent.FACTCHANGED, fc}));
							rule.addEvent(new EventType(new String[]{ChangeEvent.BELIEFCHANGED, fc}));
						}
						rulesystem.getRulebase().addRule(rule);
					}
					
					List<MGoal> gfs = trigger.getGoalFinisheds();
					if(gfs!=null && gfs.size()>0)
					{
						Rule<Void> rule = new Rule<Void>("create_plan_goalfinished_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
						for(MGoal gf: gfs)
						{
							rule.addEvent(new EventType(new String[]{ChangeEvent.GOALDROPPED, gf.getName()}));
						}
						rulesystem.getRulebase().addRule(rule);
					}
					
					final MCondition mcond = trigger.getCondition();
					if(mcond!=null)
					{
						Rule<Void> rule = new Rule<Void>("create_plan_condition_"+mplan.getName(), new CombinedCondition(new ICondition[]{new NotInShutdownCondition(), new ICondition()
						{
							public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
							{
								UnparsedExpression uexp = mcond.getExpression();
								Boolean ret = (Boolean)SJavaParser.parseExpression(uexp, IInternalBDIAgentFeature.get().getBDIModel().getModelInfo().getAllImports(), IInternalBDIAgentFeature.get().getClassLoader())
										.getValue(IExecutionFeature.get().getComponent().getFeature(IModelFeature.class).getFetcher());
								return new Future<Tuple2<Boolean, Object>>(ret!=null && ret.booleanValue()? TRUE: FALSE);
							}
						}}), createplan);
						rule.setEvents(mcond.getEvents());
						rulesystem.getRulebase().addRule(rule);
					}
				}
				
				// context condition
								
//				final MethodInfo mi = mplan.getBody().getContextConditionMethod(component.getClassLoader());
//				if(mi!=null)
//				{
//					PlanContextCondition pcc = mi.getMethod(component.getClassLoader()).getAnnotation(PlanContextCondition.class);
//					String[] evs = pcc.beliefs();
//					RawEvent[] rawevs = pcc.rawevents();
//					List<EventType> events = new ArrayList<EventType>();
//					for(String ev: evs)
//					{
//						BDIAgentFeature.addBeliefEvents(component, events, ev);
//					}
//					for(RawEvent rawev: rawevs)
//					{
//						events.add(BDIAgentFeature.createEventType(rawev));
//					}f
//				
//					IAction<Void> abortplans = new IAction<Void>()
//					{
//						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
//						{
//							Collection<RPlan> coll = rcapa.getPlans(mplan);
//							
//							for(final RPlan plan: coll)
//							{
//								invokeBooleanMethod(plan.getBody().getBody(), mi.getMethod(component.getClassLoader()), plan.getModelElement(), event, plan, component)
//									.addResultListener(new IResultListener<Boolean>()
//								{
//									public void resultAvailable(Boolean result)
//									{
//										if(!result.booleanValue())
//										{
//											plan.abort();
//										}
//									}
//									
//									public void exceptionOccurred(Exception exception)
//									{
//									}
//								});
//							}
//							return IFuture.DONE;
//						}
//					};
//					
//					Rule<Void> rule = new Rule<Void>("plan_context_abort_"+mplan.getName(), 
//						new PlansExistCondition(mplan, rcapa), abortplans);
//					rule.setEvents(events);
//					rulesystem.getRulebase().addRule(rule);
//				}
//				else 
					
				if(mplan.getContextCondition()!=null)
				{
					final MethodInfo mi = mplan.getBody().getContextConditionMethod(IInternalBDIAgentFeature.get().getClassLoader());
					final Method m = mi!=null? mi.getMethod(IInternalBDIAgentFeature.get().getClassLoader()): null;
					final MCondition mcond = mplan.getContextCondition();
					
					IAction<Void> abortplans = new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							Collection<RPlan> coll = rcapa.getPlans(mplan);
							
							for(final RPlan plan: coll)
							{
								if(m!=null)
								{
									if(!invokeBooleanMethod(plan.getBody().getBody(), mi.getMethod(IInternalBDIAgentFeature.get().getClassLoader()), plan.getModelElement(), event, plan))
									{
										plan.abort();
									}
								}
								else
								{
									if(!evaluateCondition(mcond, plan.getModelElement(), Collections.singletonMap(plan.getFetcherName(), (Object)plan)))
									{
										plan.abort();
									}
								}
							}
							return IFuture.DONE;
						}
					};
					
					Rule<Void> rule = new Rule<Void>("plan_context_condition_"+mplan.getName(), new PlansExistCondition(mplan, rcapa), abortplans);
					rule.setEvents(mcond.getEvents());
					rulesystem.getRulebase().addRule(rule);
				}
			}
			
			// add/rem goal inhibitor rules
			if(!goals.isEmpty())
			{
				boolean	usedelib	= false;
				for(int i=0; !usedelib && i<goals.size(); i++)
				{
					usedelib	= goals.get(i).getDeliberation()!=null;
				}
				
				final IDeliberationStrategy delstr = new EasyDeliberationStrategy();
				delstr.init();
				RCapability capa = IInternalBDIAgentFeature.get().getCapability();
				capa.setDeliberationStrategy(delstr);

				if(usedelib)
				{
					List<EventType> events = new ArrayList<EventType>();
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, EventType.MATCHALL}));
					Rule<Void> rule = new Rule<Void>("goal_addinitialinhibitors", 
						ICondition.TRUE_CONDITION, new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							// create the complete inhibitorset for a newly adopted goal
							RGoal goal = (RGoal)event.getContent();
							return delstr.goalIsAdopted(goal);
						}
					});
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
					
					events = new ArrayList<EventType>();
					events.add(new EventType(new String[]{ChangeEvent.GOALDROPPED, EventType.MATCHALL}));
					rule = new Rule<Void>("goal_removegoalfromdelib", 
						ICondition.TRUE_CONDITION, new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							// Remove a goal completely from 
							RGoal goal = (RGoal)event.getContent();
							return delstr.goalIsDropped(goal);
						}
					});
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
					
					events = BDIAgentFeature.getGoalEvents(null);
					rule = new Rule<Void>("goal_addinhibitor", 
						new ICondition()
						{
							public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
							{
								// return true when other goal is active and inprocess
								boolean ret = false;
								EventType type = event.getType();
								RGoal goal = (RGoal)event.getContent();
								ret = ChangeEvent.GOALACTIVE.equals(type.getType(0)) && RGoal.GoalProcessingState.INPROCESS.equals(goal.getProcessingState())
									|| (ChangeEvent.GOALINPROCESS.equals(type.getType(0)) && RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState()));
//									return ret? ICondition.TRUE: ICondition.FALSE;
								return new Future<Tuple2<Boolean,Object>>(ret? ICondition.TRUE: ICondition.FALSE);
							}
						}, new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							RGoal goal = (RGoal)event.getContent();
							return delstr.goalIsActive(goal);
						}
					});
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
					
					rule = new Rule<Void>("goal_removeinhibitor", 
						new ICondition()
						{
							public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
							{
//								if(getComponentIdentifier().getName().indexOf("Ambu")!=-1)
//									System.out.println("remin");
								
								// return true when other goal is active and inprocess
								boolean ret = false;
								EventType type = event.getType();
								if(event.getContent() instanceof RGoal)
								{
									RGoal goal = (RGoal)event.getContent();
									ret = ChangeEvent.GOALSUSPENDED.equals(type.getType(0)) 
										|| ChangeEvent.GOALOPTION.equals(type.getType(0))
//										|| ChangeEvent.GOALDROPPED.equals(type.getType(0)) 
										|| !RGoal.GoalProcessingState.INPROCESS.equals(goal.getProcessingState());
								}
//									return ret? ICondition.TRUE: ICondition.FALSE;
								return new Future<Tuple2<Boolean,Object>>(ret? ICondition.TRUE: ICondition.FALSE);
							}
						}, new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							// Remove inhibitions of this goal 
							RGoal goal = (RGoal)event.getContent();
							return delstr.goalIsNotActive(goal);
						}
					});
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
				}
				
				Rule<Void> rule = new Rule<Void>("goal_activate", 
					new LifecycleStateCondition(RGoal.GoalLifecycleState.OPTION),
					new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						RGoal goal = (RGoal)event.getContent();
						
						// For subgoals, check if parent still adopted (hack!!!) TODO: fix connected goal/plan lifecycles!!!
//						if(goal.isAdopted())
						{
							return delstr.goalIsOption(goal);							
						}
//						else
//						{
//							return IFuture.DONE;
//						}
					}
				});
//				rule.addEvent(new EventType(new String[]{ChangeEvent.GOALNOTINHIBITED, EventType.MATCHALL}));
				rule.addEvent(new EventType(new String[]{ChangeEvent.GOALOPTION, EventType.MATCHALL}));
//				rule.setEvents(SUtil.createArrayList(new String[]{ChangeEvent.GOALNOTINHIBITED, ChangeEvent.GOALOPTION}));
				rulesystem.getRulebase().addRule(rule);
			}
			
			
			// Init must be set to true before init writes to ensure that new events
			// are executed and not processed as init writes
			IInternalBDILifecycleFeature bdil = (IInternalBDILifecycleFeature)IExecutionFeature.get().getComponent().getFeature(MicroAgentFeature.class);
			bdil.setInited(true);
			
			// After init rule execution mode to direct
			rulesystem.setQueueEvents(false);
			
//			System.out.println("inited: "+component.getComponentIdentifier());
			
			// perform init write fields (after injection of bdiagent)
			BDIAgentFeature.performInitWrites(IExecutionFeature.get().getComponent());
		}
	}
	
	/**
	 *  Extracted start behavior. 
	 */
	public static class EndBehavior extends LifecycleBehavior
	{		
		/**
		 *  Start the end behavior.
		 *  
		 *  todo: problem with events
		 *  it is unclear how to wait for processing end of internal/end events 
		 *  solution: do not allow posting end events?!
		 */
		public IFuture<Void> startEndBehavior(final IBDIModel bdimodel, final RuleSystem rulesystem, final RCapability rcapa)
		{
			final Future<Void>	ret	= new Future<Void>();
			final IInternalBDIAgentFeature bdif = IInternalBDIAgentFeature.get();
			
			// Barrier to wait for all body processing.
			FutureBarrier<Void>	bodyend	= new FutureBarrier<Void>();
						
			// Abort running goals.
			Collection<RGoal> goals = new ArrayList<>(bdif.getCapability().getGoals());
//			System.out.println(component.getComponentIdentifier()+" dropping body goals: "+goals);
			for(final RGoal goal: goals)
			{
				IFuture<Void>	fut	= goal.drop();
				bodyend.addFuture(fut);
//				fut.addResultListener(new IResultListener<Void>()
//				{
//					@Override
//					public void resultAvailable(Void result)
//					{
//						System.out.println(component.getComponentIdentifier()+" dropped body goal: "+goal);
//					}
//					
//					@Override
//					public void exceptionOccurred(Exception exception)
//					{
//						System.out.println(component.getComponentIdentifier()+" dropped body goal: "+goal+", "+exception);
//					}
//				});
			}
			
			// Abort running plans.
			Collection<RPlan> plans = bdif.getCapability().getPlans();
//			System.out.println(component.getComponentIdentifier()+" dropping body plans: "+plans);
			for(final RPlan plan: plans)
			{
				IFuture<Void>	fut	= plan.abort();
				bodyend.addFuture(fut);
//				fut.addResultListener(new IResultListener<Void>()
//				{
//					@Override
//					public void resultAvailable(Void result)
//					{
//						System.out.println(component.getComponentIdentifier()+" dropped body plan: "+plan);
//					}
//					
//					@Override
//					public void exceptionOccurred(Exception exception)
//					{
//						System.out.println(component.getComponentIdentifier()+" dropped body plan: "+plan+", "+exception);
//					}
//				});
			}
			
			bodyend.waitFor().addResultListener(new DelegationResultListener<Void>(ret)
			{
				public void customResultAvailable(Void result)
				{
//					System.out.println(component.getComponentIdentifier()+" body end");
//					String confname = component.getConfiguration();
//					if(confname!=null)
//					{
//						MConfiguration mconf = bdimodel.getCapability().getConfiguration(confname);
//						
//						if(mconf!=null)
//						{
//							final CounterResultListener<Void> lis = new CounterResultListener<Void>(3, new DelegationResultListener<Void>(ret));
//								
//							// Create end plans
//							final List<MConfigParameterElement> iplans = mconf.getEndPlans();
//							dispatchConfigPlans(component, iplans, bdimodel).addResultListener(lis);
//							
//							// Create end goals
//							final List<MConfigParameterElement> igoals = mconf.getEndGoals();
//							dispatchConfigGoals(component, igoals, bdimodel).addResultListener(lis);
//							
//							// Create end events
//							final List<MConfigParameterElement> ievents = mconf.getEndEvents();
//							dispatchConfigEvents(component, ievents, bdimodel).addResultListener(lis);
//						}
//						else
//						{
//							ret.setResult(null);
//						}
//					}
//					else
					{
						ret.setResult(null);
					}
				}
			});
			
			return ret;
		}
	}

}


