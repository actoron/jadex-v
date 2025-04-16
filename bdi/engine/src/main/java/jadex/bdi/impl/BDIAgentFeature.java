package jadex.bdi.impl;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IDeliberationStrategy;
import jadex.bdi.IGoal.GoalLifecycleState;
import jadex.bdi.impl.goal.EasyDeliberationStrategy;
import jadex.bdi.impl.goal.RGoal;
import jadex.bdi.impl.plan.RPlan;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.execution.impl.ILifecycle;
import jadex.future.Future;
import jadex.future.IFuture;
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
	protected Set<RPlan>	plans;
	
	/** The currently adopted goals. */
	protected Map<Class<?>, Set<RGoal>>	goals;
	
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
	public void onStart()
	{
		((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).addStepListener(new BDIStepListener(/*rulesystem*/));
		
		// Initiate goal deliberation
		final IDeliberationStrategy delstr = new EasyDeliberationStrategy();
		delstr.init();
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
	
	@Override
	public void onEnd()
	{
	}
	
	//-------- IBDIAgentFeature interface --------

	/**
	 *  Dispatch a pojo goal and optionally wait for its result.
	 *  @param goal The pojo goal.
	 *  @return The goal result.
	 */
	public <T, E> IFuture<E> dispatchTopLevelGoal(final T goal)
	{
//		final MGoal mgoal = ((MCapability)capa.getModelElement()).getGoal(goal.getClass().getName());
//		if(mgoal==null)
//			throw new RuntimeException("Unknown goal type: "+goal);
		// TODO: Capability context!?
		final RGoal rgoal = new RGoal(goal, null, self, Collections.singletonList(self.getPojo()));
//		
//		rgoal.addListener(new ExceptionDelegationResultListener<Void, E>(ret)
//		{
//			public void customResultAvailable(Void result)
//			{
//				Object res = RGoal.getGoalResult(rgoal, self.getPojo().getClass().getClassLoader());
//				ret.setResult((E)res);
//			}
//		});
//
		// Adopt directly (no schedule step)
		// TODO: why step for subgoal and not for top-level!?
		rgoal.adopt(null);
		
		@SuppressWarnings("unchecked")
		IFuture<E>	ret	= (IFuture<E>) rgoal.getFinished();
		return ret;
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
			plans	= new LinkedHashSet<>();
		}
		plans.add(rplan);
		
		if(rplan.getPojo()!=null)
		{
			((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(rplan.getAllPojos(), rplan, contextfetchers);
		}
	}
	
	/**
	 *  Get the plans, if any.
	 */
	public Set<RPlan>	getPlans()
	{
		return plans;
	}
	
	/**
	 *  Remove a plan after it has finished.
	 */
	public void removePlan(RPlan rplan, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{
		plans.remove(rplan);

		if(rplan.getPojo()!=null)
		{
			((InjectionFeature)self.getFeature(IInjectionFeature.class)).removeExtraObject(rplan.getAllPojos(), rplan, contextfetchers);
		}
	}

	
	/**
	 *  Add a Goal.
	 *  Called when the goal is adopted.
	 */
	public void addGoal(RGoal rgoal, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
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
		
		((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(rgoal.getAllPojos(), rgoal, contextfetchers);
	}
	
	/**
	 *  Get the goals of the given type, if any.
	 */
	public Set<RGoal>	getGoals(Class<?> goaltype)
	{
		return goals!=null ? goals.get(goaltype) : null;
	}
	
	/**
	 *  Remove a Goal after it is dropped.
	 */
	public void removeGoal(RGoal rgoal, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{
		goals.get(rgoal.getPojo().getClass()).remove(rgoal);
		((InjectionFeature)self.getFeature(IInjectionFeature.class)).removeExtraObject(rgoal.getAllPojos(), rgoal, contextfetchers);
	}
}
