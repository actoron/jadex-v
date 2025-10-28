package jadex.bdi.impl.goal;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jadex.bdi.GoalDroppedException;
import jadex.bdi.GoalFailureException;
import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IGoal;
import jadex.bdi.IPlan.GoalFinishedEvent;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.BDIModel;
import jadex.bdi.impl.IDeliberationStrategy;
import jadex.bdi.impl.PlanAborted;
import jadex.bdi.impl.goal.MGoal.IGoalConditionAction;
import jadex.bdi.impl.plan.ExecutePlanStepAction;
import jadex.bdi.impl.plan.RPlan;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.injection.impl.InjectionFeature;
import jadex.injection.impl.InjectionFeature.ErrorHolder;

/**
 *  Goal instance implementation.
 */
public class RGoal extends /*RFinishableElement*/RProcessableElement implements IGoal//, IInternalPlan
{
	//-------- attributes --------
	
	/** The lifecycle state. */
	protected GoalLifecycleState lifecyclestate;

	/** The processing state. */
	protected GoalProcessingState processingstate;

	/** The parent plan (if subgoal). */
	protected RPlan parentplan;
//	protected RGoal parentgoal;
	
	/** The child plan. */
	protected RPlan childplan;
	
//	/** The candidate from which this plan was created. Used for tried plans in proc elem. */
//	protected ICandidateInfo candidate;
	
	/** The finished future (if someone waits for the goal). */
	public TerminableFuture<Object>	finished;
	
	/** The goal result, if this is a query goal. */
	Object	result	= null;
	
	/** Remember last plan exception to pass on in case goal fails due to no more plans. */
	protected Exception	exception;
	
	protected MGoal	mgoal;
	
	/** Set to true when goal is added to agent.
	 *  Used to check if goal needs to be removed when dropped. */
	// TODO: better way? -> cf. GoalFlickerTest
	protected boolean	adopted;
	
	
	/** Increase means-end reasoning cycle number whenever goal gets (re)activated.
	 *  Used to ignore outdated plans and means-end reasoning actions. */
	// TODO: better way? -> cf. GoalFlickerTest
	protected int mrcycle	= 0;
	
	
	//-------- constructors --------
	
	/**
	 *  Create a new rgoal. 
	 *  @param pojo	The pojo goal.
	 *  @param parant	The Plan (if subgoal) or null.
	 */
	public RGoal(Object pojogoal, RPlan parent, IComponent comp)
	{
		// Hack!!! set parent pojos (i.e. capability) later.
		super(pojogoal, comp, null);
		this.lifecyclestate = GoalLifecycleState.NEW;
		this.processingstate = GoalProcessingState.IDLE;
		this.comp	= comp;
		this.parentplan	= parent;
		
		this.mgoal	= ((BDIAgentFeature)getComponent().getFeature(IBDIAgentFeature.class)).getModel().getGoalInfo(pojogoal.getClass());
		if(mgoal==null)
		{
			throw new IllegalArgumentException("Unknown goal type (missing @Goal annotation?): "+pojogoal.getClass());
		}
		
		this.parentpojos	= ((BDIAgentFeature)getComponent()
				.getFeature(IBDIAgentFeature.class)).getCapability(mgoal.parentclazzes());

	}
	
	//-------- methods --------
	
	/**
	 *  Get the pojo of the goal
	 */
	public Object	getPojo()
	{
		return pojoelement;
	}

	/**
	 *  Get the parentplan.
	 *  @return The parentplan.
	 */
	public RPlan getParentPlan()
	{
		return parentplan;
	}
	
//	/**
//	 *  Get the parentgoal.
//	 *  @return The parentgoal.
//	 */
//	public RGoal getParentGoal()
//	{
//		return parentgoal;
//	}
//	
//	/**
//	 *  Get parent (goal or plan).
//	 */
//	public Object	getParent()
//	{
//		return parentplan!=null? parentplan: parentgoal;
//	}
	
	/** Means-end reasoning cycle number.
	 *  Used to ignore outdated plans and means-end reasoning actions.
	 */
	public int	getMRCycle()
	{
		return mrcycle;
	}


	/**
	 *  Get the lifecycleState.
	 *  @return The lifecycleState.
	 */
	public GoalLifecycleState getLifecycleState()
	{
		return lifecyclestate;
	}

	/**
	 *  Set the lifecycleState.
	 *  @param lifecycleState The lifecycleState to set.
	 */
	public void doSetLifecycleState(GoalLifecycleState lifecyclestate)
	{
//		System.out.println("lifecyle state: "+getId()+", "+lifecyclestate+", "+getPojo()+", "+getParentPlan());
		
		this.lifecyclestate = lifecyclestate;
	}

	/**
	 *  Get the processingState.
	 *  @return The processingState.
	 */
	public GoalProcessingState getProcessingState()
	{
		return processingstate;
	}

	/**
	 *  Set the processingState.
	 *  @param processingState The processingState to set.
	 */
	public void doSetProcessingState(GoalProcessingState processingstate)
	{
//		System.out.println("proc state: "+getId()+", "+processingstate+", "+getPojo()+", "+getParentPlan());
		
		this.processingstate = processingstate;
	}
	
	/**
	 *  Set the processingState.
	 *  @param processingState The processingState to set.
	 */
	public void setProcessingState(GoalProcessingState processingstate)
	{
		if(getProcessingState().equals(processingstate))
			return;
		
		if(isFinished())
		{
			throw new RuntimeException("Final proc state cannot be changed: "+getProcessingState()+" "+processingstate+", "+this);
		}
			
		// If was inprocess -> now stop processing.
		if(!GoalProcessingState.INPROCESS.equals(processingstate))
		{
			// Reset APL.
			setApplicablePlanList(null);
			
			// Clean tried plans if necessary.
			setTriedPlans(null);
		}
		
		doSetProcessingState(processingstate);
		
		// Goal notinprocess event trigger reactivation of inhibited goal
		// -> set to dropping first to abort plans and avoid reactivation
		if(isFinished())
		{
			setLifecycleState(GoalLifecycleState.DROPPING);
		}

		// If now is inprocess -> start processing
		if(GoalProcessingState.INPROCESS.equals(processingstate))
		{
//			getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALINPROCESS, modelname}), this));
			comp.getFeature(IExecutionFeature.class).scheduleStep(new FindApplicableCandidatesAction(this));
			IDeliberationStrategy	delstr	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getDeliberationStrategy();
			delstr.goalIsActive(this);
		}
		else
		{
//			getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALNOTINPROCESS, modelname}), this));
			IDeliberationStrategy	delstr	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getDeliberationStrategy();
			delstr.goalIsNotActive(this);
		}
	}
	
	/**
	 *  Set the lifecycle state.
	 *  @param processingState The processingState to set.
	 */
	public void setLifecycleState(GoalLifecycleState lifecyclestate)
	{
		if(lifecyclestate.equals(getLifecycleState()))
			return;
		
		if(GoalLifecycleState.DROPPED.equals(getLifecycleState()))
			throw new RuntimeException("Final proc state cannot be changed: "+getLifecycleState()+" "+lifecyclestate);
		if(GoalLifecycleState.DROPPING.equals(getLifecycleState()) && !GoalLifecycleState.DROPPED.equals(lifecyclestate))
			throw new RuntimeException("Final proc state cannot be changed: "+getLifecycleState()+" "+lifecyclestate);
		
		doSetLifecycleState(lifecyclestate);
		
		if(GoalLifecycleState.ADOPTED.equals(lifecyclestate))
		{
			// Check relevant conditions initially
			for(IGoalConditionAction cond: mgoal.query())
			{
				cond.execute(getComponent(), null, this);
			}
			for(IGoalConditionAction cond: mgoal.target())
			{
				cond.execute(getComponent(), null, this);
			}
			for(IGoalConditionAction cond: mgoal.drop())
			{
				cond.execute(getComponent(), null, this);
			}
			for(IGoalConditionAction cond: mgoal.context())
			{
				cond.execute(getComponent(), null, this);
			}
			
			if(GoalLifecycleState.ADOPTED.equals(getLifecycleState()))
			{
				IDeliberationStrategy	delstr	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getDeliberationStrategy();
				delstr.goalIsAdopted(this);
				setLifecycleState(GoalLifecycleState.OPTION);
//				getRuleSystem().addEvent(new Event(new EventType(new String[]{BDIRuleEventType.GOALADOPTED, modelname}), this));
			}
		}
		else if(GoalLifecycleState.ACTIVE.equals(lifecyclestate))
		{
			mrcycle++;
			
//			getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALACTIVE, modelname}), this));

			// start means-end reasoning unless maintain goal
			if(mgoal.maintain().isEmpty())
			{
				// Check for result if procedural query goal.
				Object	result	= null;
				if(!isDeclarative() && getPojo() instanceof Supplier<?>)
				{
					try
					{
						result	= ((Supplier<?>)getPojo()).get();
					}
					catch(Exception e)
					{
						getComponent().getLogger().log(Level.WARNING, "Exception in query goal Supplier.get() implementation: "+e);
					}
				}
				
				if(result!=null)
				{
					queryConditionTriggered(result);
				}
				else
				{
					setProcessingState(GoalProcessingState.INPROCESS);
				}
			}
			
			// If maintain goal -> set to idle first and check condition, which might set it to in process
			else
			{
				setProcessingState(GoalProcessingState.IDLE);
				for(IGoalConditionAction cond: mgoal.maintain())
				{
					cond.execute(getComponent(), null, this);
				}
			}
		}
		
		// ready to be activated via deliberation
		else if(GoalLifecycleState.OPTION.equals(lifecyclestate))
		{
			abortPlans().addResultListener(new IResultListener<Void>()
			{
				@Override
				public void resultAvailable(Void result)
				{
					// If still in same lifecycle state (goal might be reactivated to to plan abortion? cf GoalFlickerTest!?)
					if(GoalLifecycleState.OPTION.equals(getLifecycleState()))
					{
						setProcessingState(GoalProcessingState.IDLE);
	//					getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALOPTION, modelname}), RGoal.this));
						IDeliberationStrategy	delstr	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getDeliberationStrategy();
						delstr.goalIsOption(RGoal.this);
					}
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					// Should not fail?
					exception.printStackTrace();
					resultAvailable(null);	// safety-net: continue anyways
				}
			});
		}
		
		// goal is suspended (no more plan executions)
		else if(GoalLifecycleState.SUSPENDED.equals(lifecyclestate))
		{
			abortPlans().addResultListener(new IResultListener<Void>()
			{
				@Override
				public void resultAvailable(Void result)
				{
					// If still in same lifecycle state (goal might be reactivated to to plan abortion? cf GoalFlickerTest!?)
					if(GoalLifecycleState.SUSPENDED.equals(getLifecycleState()))
					{
						setProcessingState(GoalProcessingState.IDLE);
					}
//					getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALSUSPENDED, modelname}), RGoal.this));
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					// Should not fail?
					exception.printStackTrace();
					resultAvailable(null);	// safety-net: continue anyways
				}
			});
		}
		
		if(GoalLifecycleState.DROPPING.equals(lifecyclestate))
		{
			abortPlans().addResultListener(new IResultListener<Void>()
			{
				@Override
				public void resultAvailable(Void result)
				{
					IExecutionFeature.get().scheduleStep(new DropGoalAction(RGoal.this));
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					// Should not fail?
					exception.printStackTrace();
					resultAvailable(null);	// safety-net: continue anyways
				}
			});
		}
		else if(GoalLifecycleState.DROPPED.equals(lifecyclestate))
		{
			if(adopted)
			{
				((BDIAgentFeature)getComponent().getFeature(IBDIAgentFeature.class)).removeGoal(this);
				IDeliberationStrategy	delstr	= ((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).getDeliberationStrategy();
				delstr.goalIsDropped(this);
				
				triggerFinishedPlans();
			}
			
			if(finished!=null)
			{
				if(isSucceeded())
				{
					// Use undone as future is maybe terminated. 
					finished.setResultIfUndone(result);
				}
				else
				{
					// Use undone as future is maybe terminated. 
					finished.setExceptionIfUndone(exception!=null ? exception : new GoalFailureException());
				}
			}
//
//			if(getListeners()!=null)
//			{
//				if(!isFinished())
//				{
//					doSetProcessingState(GoalProcessingState.FAILED);
//					setException(new GoalDroppedException(this.toString()));
//				}
//				super.notifyListeners();
//			}
		}
	}
	
	/**
	 *  Get the finished future to wait for goal finished/result.
	 */
	public ITerminableFuture<?>	getFinished()
	{
		if(finished==null)
		{
			finished	= new TerminableFuture<>(reason -> 
			{
				exception	= reason;
//				System.out.println("drop: "+this+", "+reason);
				drop();	
			});
		}
		return finished;
	}
	
	/**
	 *  Adopt a goal.
	 */
	public void	adopt()
	{
		adopted	= true;
		
			
		((BDIAgentFeature)comp.getFeature(IBDIAgentFeature.class)).addGoal(this);
		
		setLifecycleState(RGoal.GoalLifecycleState.ADOPTED);
	}
	
	/**
	 *  Abort the child plans.
	 */
	protected IFuture<Void> abortPlans()
	{
		IFuture<Void>	ret;
		if(childplan!=null)
		{
			// If currently processing change events -> throw PlanAborted later after all listeners are done.
			ErrorHolder	holder	= InjectionFeature.LISTENER_ERROR.get();
			if(holder!=null)
			{
				try
				{
					ret	= childplan.abort();
				}
				catch(PlanAborted pa)
				{
					holder.error	= pa;
					ret	= IFuture.DONE;
				}
			}
			else
			{
				ret	= childplan.abort();
			}
		}
		else
		{
			ret	= IFuture.DONE;
		}
		return ret;
	}
	
	/**
	 *  Check if plans need to be executed when goal is finished.
	 */
	protected void triggerFinishedPlans()
	{
		BDIModel model	= ((BDIAgentFeature) getComponent().getFeature(IBDIAgentFeature.class)).getModel();
		List<ICandidateInfo>	cands	= model.getGoalFinishedPlans(getPojo().getClass());
		if(cands!=null)
		{
			for(ICandidateInfo cand: cands)
			{
				RPlan	rplan	= cand.createPlan(getComponent(), new GoalFinishedEvent(this));
				if(cand.getBody().checkPrecondition(rplan))
				{
					comp.getFeature(IExecutionFeature.class).scheduleStep(new ExecutePlanStepAction(rplan));
				}
			}
		}
	}
	
	/**
	 *  Get the model element.
	 */
	public MGoal getMGoal()
	{
		return mgoal;
	}
	
	/**
	 *  Test if the element is succeeded.
	 */
	public boolean isSucceeded()
	{
		return GoalProcessingState.SUCCEEDED.equals(processingstate);
	}
	
	/**
	 *  Test if the element is failed.
	 */
	public boolean isFailed()
	{
		return GoalProcessingState.FAILED.equals(processingstate);
	}
	
	/**
	 *  Test if the element is finished.
	 */
	public boolean isFinished()
	{
		return isSucceeded() || isFailed();
	}
//	
//	
//	/**
//	 *  Test if the goal is in lifecyclestate 'active'.
//	 */
//	// legacy v2 method.
//	public boolean isActive()
//	{
//		return lifecyclestate==GoalLifecycleState.ACTIVE;
//	}
//	
	/**
	 *  Get the childplan.
	 *  @return The childplan.
	 */
	public RPlan getChildPlan()
	{
		return childplan;
	}

	/**
	 *  Set the childplan.
	 *  @param childplan The childplan to set.
	 */
	public void setChildPlan(RPlan childplan)
	{
		this.childplan = childplan;
	}
//	
//	/**
//	 *  Get the hashcode.
//	 */
//	public int hashCode()
//	{
//		int ret;
//		if(getMGoal().isUnique())
//		{
//			if(getPojo()!=null)
//			{
//				ret = getPojo().hashCode();
//			}
//			else
//			{
//				MGoal mgoal	= (MGoal)getModelElement();
//				ret = 31 + mgoal.hashCode();
//				if(mgoal.getParameters()!=null)
//				{
//					for(MParameter param: mgoal.getParameters())
//					{
//						if(mgoal.getExcludes()==null || !mgoal.getExcludes().contains(mgoal.getName()))
//						{
//							if(!param.isMulti(((MicroAgent)getAgent()).getClassLoader()))
//							{
//								Object val = getParameter(param.getName()).getValue();
//								ret = 31*ret + (val==null? 0: val.hashCode());
//							}
//							else
//							{
//								Object[] vals = getParameterSet(param.getName()).getValues();
//								ret = 31*ret + (vals==null? 0: Arrays.hashCode(vals));
//							}
//						}
//					}
//				}
//			}
//		}
//		else
//		{
//			ret = super.hashCode();
//		}
//		return ret;
//	}
//
//	/**
//	 *  Test if equal to other object.
//	 */
//	public boolean equals(Object obj)
//	{
//		boolean ret = false;
//		if(obj instanceof RGoal)
//		{
//			RGoal other = (RGoal)obj;
//			if(getMGoal().isUnique())
//			{
//				if(getPojo()!=null)
//				{
//					ret = getPojo().equals(other.getPojo());
//				}
//				else
//				{
//					ret = isSame(other);
//				}
//			}
//			else
//			{
//				ret = super.equals(obj);
//			}
//		}
//		return ret;
////		ret = getMGoal().isUnique()? getPojoElement().equals(((RProcessableElement)obj).getPojoElement()): super.equals(obj);
//	}

	/**
	 *  Called when a plan is finished.
	 */
	public void planFinished(/*IInternalPlan*/RPlan rplan)
	{
//		// Atomic block to avoid goal conditions being triggered in between
//		// Required, e.g. for writing back parameter set values into query goal -> first add value would trigger goal target, other values would not be set.
//		boolean	queue	= IInternalBDIAgentFeature.get().getRuleSystem().isQueueEvents();
//		IInternalBDIAgentFeature.get().getRuleSystem().setQueueEvents(true);
//		
		super.planFinished(rplan);

		assert rplan==childplan;
		childplan = null;
		
		if(rplan!=null)
		{
			if(rplan.isFailed())
			{
				this.exception	= rplan.getException();
			}
		}

		// 'Procedural' query goal success semantics (i.e. pojo is supplier without query condition).
		if(!isDeclarative()	&& getPojo() instanceof Supplier<?>)
		{
			this.result	= ((Supplier<?>)getPojo()).get();
		}
		
		// Check procedural success semantics
		if(result!=null || rplan!=null && isProceduralSucceeded(rplan))
		{
			// succeeded leads to lifecycle state dropping!
			setProcessingState(isRecur() ? GoalProcessingState.PAUSED : GoalProcessingState.SUCCEEDED);
		}
		
		// Continue goal processing if still active
		if(GoalLifecycleState.ACTIVE.equals(getLifecycleState()))
		{
			// Retry if plan executed and more plans available.
			if(rplan!=null && GoalProcessingState.INPROCESS.equals(processingstate) /*&& isRetry() && RProcessableElement.State.CANDIDATESSELECTED.equals(getState())*/)
			{
				Runnable	step	= /*getMGoal().isRebuild() ? new FindApplicableCandidatesAction(this) : */new SelectCandidatesAction(this); 
//				if(getMGoal().getRetryDelay()>-1)
//				{
//					IExecutionFeature.get().waitForDelay(getMGoal().getRetryDelay())
//						.then(v -> IExecutionFeature.get().scheduleStep(step));
//				}
//				else
				{
					IExecutionFeature.get().scheduleStep(step);
				}
			}
			
			// No retry but not finished (or idle for  maintain goals). 
			else if(!isFinished() && !GoalProcessingState.IDLE.equals(getProcessingState()))
			{				
				// Recur when possible
				if(isRecur())
				{
					setProcessingState(GoalProcessingState.PAUSED);
					
					// Auto-recur, when no recur condition defined.
					if(getMGoal().recur().isEmpty())
					{
						Runnable	step	= () ->
						{
							if(GoalLifecycleState.ACTIVE.equals(getLifecycleState())
								&& GoalProcessingState.PAUSED.equals(getProcessingState()))
							{
								setProcessingState(GoalProcessingState.INPROCESS);
							}
						};
						
						if(mgoal.annotation().recurdelay()>0)
						{
							IExecutionFeature.get().waitForDelay(mgoal.annotation().recurdelay())
								.then(v -> IExecutionFeature.get().scheduleStep(step));
						}
						else
						{
							IExecutionFeature.get().scheduleStep(step);
						}
					}
					
					// else condition will trigger recur
				}
				
				// Else no more plans -> fail.
				else //if(!isRetry() || RProcessableElement.State.NOCANDIDATES.equals(getState()))
				{
					if(exception==null)
					{
						exception	= new GoalFailureException("No more candidates: "+this);
					}
					setProcessingState(GoalProcessingState.FAILED);
				}
			}
		}
		
//		IInternalBDIAgentFeature.get().getRuleSystem().setQueueEvents(queue);
	}
	
	//-------- methods that are goal specific --------

	// todo: implement those methods in goal types
	
//	/**
//	 * 
//	 */
//	public boolean isRetry()
//	{
//		return getMGoal().isRetry();
//	}
//	
	/**
	 *  Check if the recur flag is set or a recur condition is present.
	 */
	public boolean isRecur()
	{
		return mgoal.annotation().recur() || !mgoal.recur().isEmpty();
	}
	
	/**
	 *  Check if the goal is declarative, i.e. has target and/or maintain condition.
	 */
	public boolean isDeclarative()
	{
		return !mgoal.query().isEmpty() || !mgoal.target().isEmpty() || !mgoal.maintain().isEmpty(); 
	}
	
	/**
	 *  Test if a goal has succeeded with respect to its plan execution.
	 */
	public boolean isProceduralSucceeded(RPlan last)
	{
		boolean ret = false;
		
		// todo: perform goals
		if(!(isDeclarative() || getPojo() instanceof Supplier<?>) 
			&& getTriedPlans()!=null && !getTriedPlans().isEmpty())
		{
			// OR case
			if(mgoal.annotation().orsuccess())
			{
				ret = last.isPassed();
			}
			// AND case
			else
			{
				// No further candidate? Then is considered as succeeded
				// todo: is it sufficient that one plan has succeeded or all?
				// todo: what to do when rebuild?
				if(getApplicablePlanList().isEmpty())
				{
					Set<RPlan> tps = getTriedPlans();
					if(tps!=null && !tps.isEmpty())
					{
						for(RPlan plan: tps)
						{
							if(plan.isPassed())
							{
								ret = true;
								break;
							}
						}
					}
				}
			}
		}
		
		return ret;
	}
	
//	/**
//	 *  Get the goal result of the pojo element.
//	 *  Searches @GoalResult and delivers value.
//	 */
//	public static Object getGoalResult(RGoal rgoal, ClassLoader cl)
//	{
//		Object ret = null;
//		Object pojo = rgoal.getPojo();
//		MGoal mgoal = rgoal.getMGoal();
//		
//		if(pojo!=null)
//		{
//			ret = pojo;
//			Object pac = mgoal.getPojoResultReadAccess(cl);
//			if(pac instanceof Field)
//			{
//				try
//				{
//					Field f = (Field)pac;
//					SAccess.setAccessible(f, true);
//					ret = f.get(pojo);
//				}
//				catch(Exception e)
//				{
//					e.printStackTrace();
//				}
//			}
//			else if(pac instanceof Method)
//			{
//				try
//				{
//					Method m = (Method)pac;
//					SAccess.setAccessible(m, true);
//					ret = m.invoke(pojo, new Object[0]);
//				}
//				catch(Exception e)
//				{
//					e.printStackTrace();
//				}
//			}
//		}
//		// xml goals
//		else
//		{
//			Map<String, Object> res = new HashMap<String, Object>(); 
//			for(IParameter param: rgoal.getParameters())
//			{
//				MParameter.Direction dir = ((MParameter)param.getModelElement()).getDirection();
//				if(MParameter.Direction.OUT.equals(dir) || MParameter.Direction.INOUT.equals(dir))
//				{
//					res.put(param.getName(), param.getValue());
//				}
//			}
//			for(IParameterSet paramset: rgoal.getParameterSets())
//			{
//				MParameter.Direction dir = ((MParameter)paramset.getModelElement()).getDirection();
//				if(MParameter.Direction.OUT.equals(dir) || MParameter.Direction.INOUT.equals(dir))
//				{
//					res.put(paramset.getName(), paramset.getValues());
//				}
//			}
//			ret = res.size()==0? null: res.size()==1? res.values().iterator().next(): res;
//		}
//		
//		return ret;
//	}
//	
	/**
	 *  Drop the goal.
	 */
	public IFuture<Void> drop()
	{
		Future<Void> ret = new Future<Void>();
		
		if(!GoalLifecycleState.DROPPING.equals(getLifecycleState()) 
			&& !GoalLifecycleState.DROPPED.equals(getLifecycleState()))
		{
			if(exception==null)
			{
				exception	= new GoalDroppedException();
			}
			getFinished()
				.then(o -> ret.setResult(null))
				.catchEx(e ->
			{
				if(e instanceof GoalDroppedException)
				{
					// Goal dropped -> mission accomplished
					ret.setResult(null);
				}
				else
				{
					ret.setException(e);
				}
			});
			setLifecycleState(GoalLifecycleState.DROPPING);
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;
	}
//	
//	/**
//	 *  Add a new listener to get notified when the goal is finished.
//	 *  @param listener The listener.
//	 */
//	// hmm? overridden to make GoalConditions test case work
//	// assumes that goal is in dropped state after waitForGaol()
//	// has been triggered
//	public void addListener(IResultListener<Void> listener)
//	{
//		if(!GoalLifecycleState.DROPPED.equals(getLifecycleState()))
//		{
//			if(listeners==null)
//				listeners = new ArrayList<IResultListener<Void>>();
//			listeners.add(listener);
//		}
//		else
//		{
//			super.addListener(listener);
//		}
//	}
//	
	/**
	 *  Called when the query condition of a goal triggers.
	 */
	public void queryConditionTriggered(Object result)
	{
		this.result	= result;
		targetConditionTriggered();
	}
	
	/**
	 *  Called when the target condition of a goal triggers.
	 */
	public void targetConditionTriggered(/*IEvent event, IRule<Void> rule, Object context*/)
	{
		// If not declarative query goal but is supplier -> store current result.
		if(result==null && getPojo() instanceof Supplier<?>)
		{
			result	= ((Supplier<?>)getPojo()).get();
		}
		
		// If has maintain condidtion -> move to idle
		if(!mgoal.maintain().isEmpty())
		{
			// Change maintain goal rule so it does not consider target condition triggered unless we move from false to true (not just true to true)
			if(GoalProcessingState.INPROCESS.equals(getProcessingState()))
			{
				abortPlans().addResultListener(new IResultListener<Void>()
				{
					@Override
					public void resultAvailable(Void result)
					{
						setProcessingState(IGoal.GoalProcessingState.IDLE);
//						// Hack! Notify finished listeners to allow for waiting via waitForGoal
//						// Cannot use notifyListeners() because it checks isSucceeded
//						if(getListeners()!=null)
//						{
//							for(IResultListener<Void> lis: getListeners())
//							{
//								lis.resultAvailable(null);
//							}
//						}
//						listeners = null;
						
						
						if(finished!=null)
						{
							// TODO: only notifies first finished -> use intermediate result for maintain goal!?
							finished.setResultIfUndone(result);
						}
					}
					
					@Override
					public void exceptionOccurred(Exception exception)
					{
						// Should not fail?
						exception.printStackTrace();
						resultAvailable(null);	// safety-net: continue anyways
					}
				});
			}
		}
		
		// No maintain condition -> goal finished
		else
		{
			setProcessingState(IGoal.GoalProcessingState.SUCCEEDED);
		}
	}
//	
//	/**
//	 * 
//	 * @param result
//	 * @param cl
//	 */
//	public void setGoalResult(Object result, ClassLoader cl)
//	{
//		setGoalResult(result, cl, null, null, null);
//	}
//	
//	/**
//	 *  Set the goal result from a plan.
//	 */
//	public void setGoalResult(Object result, ClassLoader cl, ChangeEvent<?> event, RPlan rplan, RProcessableElement rpe)
//	{
//		//System.out.println("set goal result: "+result);
//		
//		MGoal mgoal = (MGoal)getModelElement();
//		Object wa = mgoal.getPojoResultWriteAccess(cl);
//		if(wa instanceof Field)
//		{
//			try
//			{
//				BDIAgentFeature.writeParameterField(result, ((Field)wa).getName(), getPojo(), null);
//				//Field f = (Field)wa;
//				//SAccess.setAccessible(f, true);
//				//f.set(getPojoElement(), result);
//			}
//			catch(Exception e)
//			{
//				throw new RuntimeException(e);
//			}
//		}
//		else if(wa instanceof Method)
//		{
//			try
//			{
//				Method m = (Method)wa;
//				SAccess.setAccessible(m, true);
//				List<Object> res = new ArrayList<Object>();
//				res.add(result);
//				Object[] params = BDIAgentFeature.getInjectionValues(m.getParameterTypes(), m.getParameterAnnotations(), 
//					rplan!=null? rplan.getModelElement(): rpe.getModelElement(), event, rplan, rpe, res);
//				if(params==null)
//					System.out.println("Invalid parameter assignment");
//				m.invoke(getPojo(), params);
//			}
//			catch(Exception e)
//			{
//				throw new RuntimeException(e);
//			}
//		}
//	}
//	
//	/**
//	 *  Call the user finished method if available.
//	 */
//	public IFuture<Void> callFinishedMethod()
//	{
//		final Future<Void> ret = new Future<Void>();
//		
//		Object pojo = getPojo();
//		if(pojo!=null)
//		{
//			MGoal mgoal = (MGoal)getModelElement();
//			MethodInfo mi = mgoal.getFinishedMethod(((MicroAgent)getAgent()).getClassLoader());
//			if(mi!=null)
//			{
//				Method m = mi.getMethod(((MicroAgent)getAgent()).getClassLoader());
//				try
//				{
//					SAccess.setAccessible(m, true);
//					Object[] params = BDIAgentFeature.getInjectionValues(m.getParameterTypes(), m.getParameterAnnotations(), getModelElement(), null, null, this);
//					Object res = m.invoke(pojo, params);
//					if(res instanceof IFuture)
//					{
//						@SuppressWarnings("unchecked")
//						IFuture<Object>	fut	= (IFuture<Object>)res;
//						fut.addResultListener(new ExceptionDelegationResultListener<Object, Void>(ret)
//						{
//							public void customResultAvailable(Object result)
//							{
//								ret.setResult(null);
//							}
//						});
//					}
//					else
//					{
//						ret.setResult(null);
//					}
//				}
//				catch(Exception e)
//				{
//					ret.setException(e);
//				}
//			}
//			else
//			{
//				ret.setResult(null);
//			}
//		}
//		else
//		{
//			ret.setResult(null);
//		}
//		
//		return ret;
//	}
//	
//	// IInternalPlan extra methods
//	
//	/**
//	 *  Test if plan has passed.
//	 */
//	public boolean isPassed()
//	{
//		return isSucceeded();
//	}
//	
//	/**
//	 *  Test if plan has been aborted.
//	 */
//	public boolean isAborted()
//	{
//		boolean aborted = false;
//		// methode is part of plan api (goal treated as plan)
//		// hence one can check if there is a plan above that was aborted
//		if(getParentGoal()!=null && getParentGoal().getParentPlan()!=null)
//		{
//			RPlan plan = getParentGoal().getParentPlan();
//			aborted = plan.isAborted();
//		}
//		return aborted;
//	}
//	
//	/**
//	 *  Check if the element is currently part of the agent's reasoning.
//	 *  E.g. the bases are always adopted and all of their contents such as goals, plans and beliefs.
//	 */
//	public boolean	isAdopted()
//	{
//		boolean	ret	= super.isAdopted() 
//			&& (getParent()==null || getParent().isAdopted()); 	// Hack!!! Subgoals removed to late, TODO: fix hierarchic goal plan lifecycle management
//		if(ret)
//		{
//			ret	= IInternalBDIAgentFeature.get().getGoals().contains(this);
//		}
//		return ret;
//	}
//
//	
//	/**
//	 *  Check if the goal is the same as another goal
//	 *  with respect to uniqueness settings.
//	 *  When two goals are the same this does not mean
//	 *  the objects are equal() in the Java sense!
//	 */
//	public boolean isSame(IGoal goal)
//	{
//		// Goals are only the same when they are of same type.
//		boolean	same	= getModelElement().equals(goal.getModelElement());
//		
//		if(same)
//		{
//			// Check parameter correspondence of goal.
//			MGoal mgoal	= (MGoal)goal.getModelElement();
//
//			if(mgoal.getParameters()!=null)
//			{
//				for(MParameter param: mgoal.getParameters())
//				{
//					if(!param.isMulti(((MicroAgent)getAgent()).getClassLoader()))
//					{
//						// Compare parameter values.
//						// Todo: Catch exceptions on parameter access?
//						Object	val1	= this.getParameter(param.getName()).getValue();
//						Object	val2	= goal.getParameter(param.getName()).getValue();
//						same	= val1==val2 || val1!=null && val1.equals(val2);
//					}
//					else
//					{
//						// Compare parameter set values.
//						// Todo: Catch exceptions on parameter set access?
//						Object[] vals1 = this.getParameterSet(param.getName()).getValues();
//						Object[] vals2 = goal.getParameterSet(param.getName()).getValues();
//						same = vals1.length==vals2.length;
//						for(int j = 0; same && j < vals1.length; j++)
//						{
//							same = vals1[j] == vals2[j] || vals1[j] != null && vals1[j].equals(vals2[j]);
//						}
//					}
//				}
//			}
//		}
//
//		return same;
//	}
//	
//	/**
//	 *  Test if a querygoal is finished.
//	 *  It is finished when all out parameters/sets are filled with a value.
//	 */
//	public static boolean isQueryGoalFinished(RGoal goal)
//	{
//		boolean ret = true;
//		
//		for(IParameter param: goal.getParameters())
//		{
//			if(!((MParameter)param.getModelElement()).isOptional())
//			{
//				Direction dir = ((MParameter)param.getModelElement()).getDirection();
//				if(MParameter.Direction.OUT.equals(dir) || MParameter.Direction.INOUT.equals(dir))
//				{
//					Object val = param.getValue();
//					ret = val!=null;
//					if(!ret)
//						break;
//				}
//			}
//		}
//		
//		if(ret)
//		{
//			for(IParameterSet paramset: goal.getParameterSets())
//			{
//				if(!((MParameter)paramset.getModelElement()).isOptional())
//				{
//					Direction dir = ((MParameter)paramset.getModelElement()).getDirection();
//					if(MParameter.Direction.OUT.equals(dir) || MParameter.Direction.INOUT.equals(dir))
//					{
//						Object[] vals = paramset.getValues();
//						ret = vals.length>0;
//						if(!ret)
//							break;
//					}
//				}
//			}
//		}
//		
////		if(ret)
////			System.out.println("query finished: "+goal);
//		
////		System.out.println("querygoal check: "+ret+" "+goal);
//		
//		return ret;
//	}
//	
//	/**
//	 *  Get the candidate.
//	 *  @return The candidate.
//	 */
//	public ICandidateInfo getCandidate()
//	{
//		return candidate;
//	}
//
//	/**
//	 *  Set the candidate.
//	 *  @param candidate The candidate to set.
//	 */
//	public void setCandidate(ICandidateInfo candidate)
//	{
//		this.candidate = candidate;
//	}
	
	@Override
	public String toString()
	{
		return super.toString() + ", " +lifecyclestate + ", " + processingstate + ", " +parentplan;
	}
}
