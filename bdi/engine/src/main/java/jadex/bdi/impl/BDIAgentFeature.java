package jadex.bdi.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IGoal;
import jadex.bdi.IGoal.GoalLifecycleState;
import jadex.bdi.impl.goal.EasyDeliberationStrategy;
import jadex.bdi.impl.goal.RGoal;
import jadex.bdi.impl.plan.IPlanBody;
import jadex.bdi.impl.plan.RPlan;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.core.IChangeListener;
import jadex.core.impl.ILifecycle;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.IValueFetcherCreator;
import jadex.injection.impl.InjectionFeature;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IAction;
import jadex.rules.eca.ICondition;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.IRule;
import jadex.rules.eca.Rule;
import jadex.rules.eca.RuleSystem;

public class BDIAgentFeature implements IBDIAgentFeature, ILifecycle
{
	/** The component. */
	protected BDIAgent	self;
	
	/** The BDI model. */
	protected BDIModel	model;
	
	/** The rule system. */
	protected RuleSystem	rulesystem;
	
	/** The currently running plans. */
	protected Map<IPlanBody, Set<RPlan>>	plans;
	
	/** The currently adopted goals. */
	protected Map<Class<?>, Set<RGoal>>	goals;
	
	/** The capabilities. */
	protected Map<List<Class<?>>, List<Object>>	capabilities;
	
	/**
	 *  Create the feature.
	 */
	public BDIAgentFeature(BDIAgent self)
	{
		this.self	= self;
		this.model	= BDIModel.getModel(self.getPojo().getClass());
		// TODO: true in old version but processEvents called for belief write!?
		this.rulesystem	= new RuleSystem(self, false)
		{
			@Override
			public IFuture<Void> addEvent(IEvent event)
			{
				// Avoid plan self-abort inside event processing
				RPlan	rplan	= RPlan.RPLANS.get();
				if(rplan!=null && !rplan.isAtomic())
				{
					try
					{
						rplan.startAtomic();
						return super.addEvent(event);
					}
					finally
					{
						rplan.endAtomic();
					}
				}
				
				else
				{
					return super.addEvent(event);
				}
			}
		};
	}
	
	//-------- ILifecycle interface --------
	
	@Override
	public void init()
	{
		((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).addStepListener(new BDIStepListener(/*rulesystem*/));
	}
	
	@Override
	public void cleanup()
	{
		// plan abort moved to overridden BDIAgent.terminate()
		// to call abort() before onend() for plans
	}
	
	protected void	abortPlans()
	{
		// Abort all plans (terminates wait futures if any)
		// TODO: generic future handler to terminate any futures on component end
		if(plans!=null)
		{
			for(Set<RPlan> planset: plans.values())
			{
				if(!planset.isEmpty())
				{
					for(RPlan plan: planset.toArray(new RPlan[planset.size()]))
					{
						plan.abort();
						// call aborted explicitly, because any wakeup code from plan.abort()
						// is cancelled by execution feature with StepAborted
						plan.getBody().callAborted(plan);
					}
				}
			}
		}
	}
	
	//-------- IBDIAgentFeature interface --------

	@Override
	public <T> ITerminableFuture<T> dispatchTopLevelGoal(Supplier<T> goal)
	{
		@SuppressWarnings("unchecked")
		ITerminableFuture<T>	ret	= (ITerminableFuture<T>)dispatchTopLevelGoal((Object)goal);
		return ret;
	}
	
	@Override
	public ITerminableFuture<Void> dispatchTopLevelGoal(Object goal)
	{
		final RGoal rgoal = new RGoal(goal, null, self);
		
		// Adopt directly (no schedule step)
		// TODO: why step for subgoal and not for top-level!?
		rgoal.adopt();
		
		@SuppressWarnings("unchecked")
		ITerminableFuture<Void>	ret	= (ITerminableFuture<Void>) rgoal.getFinished();
		return ret;
	}
	
	@Override
	public void dropGoal(Object goal)
	{
		for(RGoal rgoal: doGetGoals(goal.getClass()))
		{
			if(goal.equals(rgoal.getPojo()))
			{
				rgoal.drop();
				break;
			}
		}
	}
	
	@Override
	public <T> Set<T> getGoals(Class<T> clazz)
	{
		Set<T> ret	= null;
		Set<RGoal>	goals	= doGetGoals(clazz);
		if(goals!=null)
		{
			ret	= new LinkedHashSet<>();
			for(RGoal goal: goals)
			{
				@SuppressWarnings("unchecked")
				T	pojo	= (T)goal.getPojo();
				ret.add(pojo);
			}
		}
		return ret==null ? Collections.emptySet() : ret;
	}
	
	@Override
	public Collection<IGoal> getGoals()
	{
		List<IGoal>	ret	= new ArrayList<>();
		if(goals!=null)
		{
			for(Class<?> type: goals.keySet())
			{
				ret.addAll(goals.get(type));
			}
		}
		return ret;
	}
	
	//-------- ICapability interface --------
	
	@Override
	public <T> void addChangeListener(String name, IChangeListener listener)
	{
		self.getFeature(IInjectionFeature.class).addListener(name, listener);
	}
	
	@Override
	public <T> void removeChangeListener(String name, IChangeListener listener)
	{
		self.getFeature(IInjectionFeature.class).removeListener(name, listener);
	}
	
	//-------- internal methods --------
	
	/**
	 *  Get the rule system.
	 */
	public RuleSystem	getRuleSystem()
	{
		return this.rulesystem;
	}
	
	//-------- helper classes --------
	
	/**
	 *  Condition for checking the lifecycle state of a goal.
	 */
	public static class LifecycleStateCondition implements ICondition
	{
		/** The allowed states. */
		protected Set<GoalLifecycleState> states;
		
		/** The flag if state is allowed or disallowed. */
		protected boolean allowed;
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(GoalLifecycleState state)
		{
			this(SUtil.createHashSet(new GoalLifecycleState[]{state}));
		}
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(Set<GoalLifecycleState> states)
		{
			this(states, true);
		}
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(GoalLifecycleState state, boolean allowed)
		{
			this(SUtil.createHashSet(new GoalLifecycleState[]{state}), allowed);
		}
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(Set<GoalLifecycleState> states, boolean allowed)
		{
			this.states = states;
			this.allowed = allowed;
		}
		
		/**
		 *  Evaluate the condition.
		 */
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
			RGoal goal = (RGoal)event.getContent();
			boolean ret = states.contains(goal.getLifecycleState());
			if(!allowed)
				ret = !ret;
//				return ret? ICondition.TRUE: ICondition.FALSE;
			
//			if(ret && goal.getLifecycleState()==GoalLifecycleState.OPTION)
//			{
//				System.out.println("dfol");
//			}
			
			return new Future<Tuple2<Boolean,Object>>(ret? ICondition.TRUE: ICondition.FALSE);
		}
	}
	
	/**
	 *  Get the model.
	 */
	public BDIModel getModel()
	{
		return model;
	}
	
	/**
	 *  Add a plan.
	 *  Called when the plan is first executed.
	 */
	public void addPlan(RPlan rplan, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{		
		if(plans==null)
		{
			plans	= new LinkedHashMap<>();
		}
		Set<RPlan>	planset	= plans.get(rplan.getBody());
		if(planset==null)
		{
			planset	= new LinkedHashSet<>();
			plans.put(rplan.getBody(), planset);
		}
		planset.add(rplan);
		
		if(rplan.getPojo()!=null)
		{
			((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(rplan.getAllPojos(), rplan);
		}
	}
	
	/**
	 *  Get the plans, if any.
	 */
	public Map<IPlanBody, Set<RPlan>>	getPlans()
	{
		return plans;
	}
	
	/**
	 *  Remove a plan after it has finished.
	 */
	public void removePlan(RPlan rplan)
	{
		Set<RPlan>	planset	= plans.get(rplan.getBody());
		if(planset!=null)
		{
			planset.remove(rplan);
			// Do not remove set to avoid continuous recreation if a plan gets executed repeatedly.
		}

		if(rplan.getPojo()!=null)
		{
			((InjectionFeature)self.getFeature(IInjectionFeature.class)).removeExtraObject(rplan.getAllPojos(), rplan);
		}
	}

	
	/**
	 *  Add a Goal.
	 *  Called when the goal is adopted.
	 */
	public void addGoal(RGoal rgoal)
	{		
		if(goals==null)
		{
			goals	= new LinkedHashMap<>();
		}
		Set<RGoal>	typedgoals	= goals.get(rgoal.getPojo().getClass()); 
		if(typedgoals==null)
		{
			typedgoals	= new LinkedHashSet<>();
			goals.put(rgoal.getPojo().getClass(), typedgoals);
		}
		typedgoals.add(rgoal);
		
		((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(rgoal.getAllPojos(), rgoal);
	}
	
	/**
	 *  Get the goals of the given type, if any.
	 */
	public Set<RGoal>	doGetGoals(Class<?> goaltype)
	{
		return goals!=null ? goals.get(goaltype) : null;
	}
	
	/**
	 *  Remove a Goal after it is dropped.
	 */
	public void removeGoal(RGoal rgoal)
	{
		goals.get(rgoal.getPojo().getClass()).remove(rgoal);
		((InjectionFeature)self.getFeature(IInjectionFeature.class)).removeExtraObject(rgoal.getAllPojos(), rgoal);
	}
	
	/**
	 *  Start deliberation in extra step otherwise model might not be inited.
	 *  (BDI feature onStart is executed before injection feature) 
	 */
	public void	startDeliberation(boolean usedelib)
	{
		// Initiate goal deliberation
		final IDeliberationStrategy delstr = new EasyDeliberationStrategy();
		delstr.init();
		
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
			
			events = new ArrayList<EventType>();
			events.add(new EventType(new String[]{ChangeEvent.GOALACTIVE, EventType.MATCHALL}));
			events.add(new EventType(new String[]{ChangeEvent.GOALINPROCESS, EventType.MATCHALL}));
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
							|| ChangeEvent.GOALINPROCESS.equals(type.getType(0)) && RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState());
//								return ret? ICondition.TRUE: ICondition.FALSE;
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
			
			events = new ArrayList<EventType>();
			events.add(new EventType(new String[]{ChangeEvent.GOALNOTINPROCESS, EventType.MATCHALL}));
			rule = new Rule<Void>("goal_removeinhibitor", 
				ICondition.TRUE_CONDITION,
				new IAction<Void>()
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
					return delstr.goalIsOption(goal);							
				}
			});
		rule.addEvent(new EventType(new String[]{ChangeEvent.GOALOPTION, EventType.MATCHALL}));
		rulesystem.getRulebase().addRule(rule);
	}
	
	/**
	 *  Add a capability.
	 */
	protected void	addCapability(List<Object> pojos)
	{
		// Add to known sub-objects
		List<Class<?>>	pojoclazzes	= new ArrayList<>();
		for(Object pojo: pojos)
		{
			pojoclazzes.add(pojo.getClass());
		}
		if(capabilities==null)
		{
			capabilities	= new LinkedHashMap<>();
		}
		// TODO: support multiple instances of same capability?
		capabilities.put(pojoclazzes, pojos);
		
		((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(pojos, null);
	}
	
	public List<Object> getCapability(List<Class<?>> parentclazzes)
	{
		return parentclazzes.size()==1 ? Collections.singletonList(self.getPojo()) : capabilities.get(parentclazzes);
	}
}
