package jadex.bdi.impl;

import java.util.Collections;
import java.util.Set;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IDeliberationStrategy;
import jadex.bdi.IGoal.GoalLifecycleState;
import jadex.bdi.impl.goal.EasyDeliberationStrategy;
import jadex.bdi.impl.goal.RGoal;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.execution.impl.ILifecycle;
import jadex.future.Future;
import jadex.future.IFuture;
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
	
	/**
	 *  Create the feature.
	 */
	public BDIAgentFeature(BDIAgent self)
	{
		this.self	= self;
		this.model	= BDIModel.getModel(self.getPojo().getClass());
		this.rulesystem	= new RuleSystem(self);
	}
	
	//-------- ILifecycle interface --------
	
	@Override
	public void onStart()
	{
		((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).addStepListener(new BDIStepListener(rulesystem));
		
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
		final Future<E> ret = new Future<E>();
		
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
		rgoal.adopt();
		
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
}
