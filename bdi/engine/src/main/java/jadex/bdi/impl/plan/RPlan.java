package jadex.bdi.impl.plan;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jadex.bdi.GoalFailureException;
import jadex.bdi.IPlan;
import jadex.bdi.PlanFailureException;
import jadex.bdi.impl.RElement;
import jadex.bdi.impl.goal.AdoptGoalAction;
import jadex.bdi.impl.goal.ICandidateInfo;
import jadex.bdi.impl.goal.RGoal;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.execution.StepAborted;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Runtime element of a plan.
 *  
 *  todo: Currently we do not have @PlanParameter. Do we need them?
 *  They are only visible as long as the plan lives, so only plan
 *  conditions could use them.
 */
public class RPlan extends RElement/*extends RParameterElement*/ implements IPlan//, IInternalPlan
{
	/** The rplans for plan threads. */
	public static final ThreadLocal<RPlan>	RPLANS	= new ThreadLocal<RPlan>();
	
	//-------- plan states --------
	
//	public static enum PlanProcessingState
//	{
//		READY, 
//		RUNNING,
//		WAITING,
//	};
	
	public static enum PlanLifecycleState
	{
		NEW, 
		BODY,
		
		PASSING,
		FAILING,
		ABORTING,
		
		PASSED,
		FAILED,
		ABORTED,
	};
	
	/** The plan has a reason. */
	protected Object reason;

//	/** The plan has a dispatched element (current goal/event). */
//	protected Object dispatchedelement;
//	
	/** The plan has subgoals attribute. */
	protected Set<RGoal> subgoals;

//	/** The plan has a wait abstraction attribute. */
//	protected WaitAbstraction waitabstraction;
//		
//	/** The plan has a waitqueue wait abstraction attribute. */
//	protected WaitAbstraction waitqueuewa;
//	
//	/** The waitqueue. */
//	protected Waitqueue waitqueue;
//	
//	/** The blocking resume. */
//	protected ICommand<ResumeCommandArgs> resumecommand;
//	
//	/** The non-blocking resumes. */
//	protected List<ICommand<ResumeCommandArgs>> resumecommands;
//	
	/** The exception thrown in body (if any). */
	protected Exception exception;
//	
//	/** The result. */
//	protected Object result;
	
	/** The plan has lifecycle state attribute. */
	protected PlanLifecycleState lifecyclestate;
	
//	/** The plan has processing state attribute (ready or waiting). */
//	protected PlanProcessingState processingstate;
	
	/** The plan body. */
	protected IPlanBody body;
	
	/** The candidate from which this plan was created. Used for tried plans in proc elem. */
	protected ICandidateInfo candidate;
	
//	/** The plan listeners. */
//	protected List<IResultListener<Object>> listeners;
//	
//	/** The wait cnt for rule names. */
//	protected int cnt;
//	
	/** The atomic flag. */
	protected boolean atomic;
	
	/** The finished future (if finishing or finished). */
	public Future<Void>	finished;
	
//	/**
//	 *  Create a new rplan based on an mplan.
//	 *  
//	 *  Reason is Object (not RProcessableElement) because it can be also ChangeEvent
//	 */
//	public static RPlan createRPlan(/*MPlan mplan, */ICandidateInfo candidate, Object reason/*, Map<String, Object> binding*/)
//	{
//		RPlan	ret	= null;
//		MBody mbody = mplan.getBody();
//		
//		IPlanBody body = null;
//
////		if(candidate.getRawCandidate().getClass().isAnnotationPresent(Plan.class))
////		{
////			body = new ClassPlanBody(rplan, candidate.getRawCandidate());
////		}
////		else if(mbody.getClazz()!=null && mbody.getServiceName()==null)
////		{
////			Class<?> clazz0 = (Class<?>)mbody.getClazz().getType(ia.getClassLoader());
////			Class<?> clazz	= clazz0;
////			while(body==null && !Object.class.equals(clazz))
////			{
////				if(clazz.isAnnotationPresent(Plan.class))
////				{
////					body = new ClassPlanBody(rplan, clazz0);
////				}
//////				else if(clazz.isAnnotationPresent(Agent.class))
//////				{
//////					body = new ComponentPlanBody(clazz0.getName()+".class", rplan);
//////				}
////				else
////				{
////					clazz	= clazz.getSuperclass();
////				}
////			}
////			
////			if(body==null)
////			{
////				throw new RuntimeException("Neither @Plan nor @Agent annotation on plan body class: "+mbody.getClazz());
////			}
////		}
////		else if(mbody.getMethod()!=null)
//		{
//			Method met = mbody.getMethod().getMethod(ia.getClassLoader());
//			body = new MethodPlanBody(rplan, met);
//			rplan = new RPlan(mplan, candidate, reason/*, mappingvals*/); //mappingvals==null? new RPlan(mplan, candidate, ia): 
//		}
//////		else if(mbody.getComponent()!=null)
//////		{
//////			body	= new ComponentPlanBody(mbody.getComponent(), ia, rplan);
//////		}
//		
//		if(body==null)
//			throw new RuntimeException("Plan body not created: "+rplan);
//		
////		MTrigger wqtr = mplan.getWaitqueue();
////		if(wqtr!=null)
////		{
////			List<EventType> events = new ArrayList<EventType>();
////			
////			for(String belname: SUtil.notNull(wqtr.getFactAddeds()))
////			{
////				events.add(new EventType(ChangeEvent.FACTADDED, belname));
////			}
////			for(String belname: SUtil.notNull(wqtr.getFactRemoveds()))
////			{
////				events.add(new EventType(ChangeEvent.FACTREMOVED, belname));
////			}
////			for(String belname: SUtil.notNull(wqtr.getFactChangeds()))
////			{
////				events.add(new EventType(ChangeEvent.FACTCHANGED, belname));
////			}			
////			for(MGoal goal: SUtil.notNull(wqtr.getGoalFinisheds()))
////			{
////				events.add(new EventType(ChangeEvent.GOALDROPPED, goal.getName()));
////			}
////			
////			if(!events.isEmpty())
////			{
////				rplan.setupEventsRule(events);
////			}
////			
////			for(MMessageEvent mevent: SUtil.notNull(wqtr.getMessageEvents()))
////			{
////				WaitAbstraction wa = rplan.getOrCreateWaitqueueWaitAbstraction();
////				wa.addModelElement(mevent);
////			}
////		}
//		
//		rplan.setBody(body);
//		
//		return rplan;
//	}
	
	/**
	 *  Create a new plan.
	 */
	public RPlan(/*MPlan mplan,*/ ICandidateInfo candidate, String name, Object reason/*, Map<String, Object> mappingvals)*/
		, IPlanBody body, IComponent comp, List<Object> pojos)
	{
//		super(mplan, mappingvals);
		super(name, null, comp, pojos);
		this.candidate = candidate;
		this.reason = reason;
		this.body	= body;
		setLifecycleState(PlanLifecycleState.NEW);
//		setProcessingState(PlanProcessingState.READY);
	}
	
//	/**
//	 *  Get the processingState.
//	 *  @return The processingState.
//	 */
//	public PlanProcessingState getProcessingState()
//	{
//		return processingstate;
//	}
//
//	/**
//	 *  Set the processingState.
//	 *  @param processingState The processingState to set.
//	 */
//	public void setProcessingState(PlanProcessingState processingstate)
//	{
//		this.processingstate = processingstate;
//	}

	/**
	 *  Get the lifecycleState.
	 *  @return The lifecycleState.
	 */
	public PlanLifecycleState getLifecycleState()
	{
		return lifecyclestate;
	}

	/**
	 *  Set the lifecycleState.
	 *  @param lifecycleState The lifecycleState to set.
	 */
	public void setLifecycleState(PlanLifecycleState lifecyclestate)
	{
		assert this.lifecyclestate!=lifecyclestate;
		
//		// Cleanup previous lifecycle phase
//		if(subgoals!=null)
//		{
//			for(IGoal subgoal: subgoals)
//			{
//				// Todo: wait for goals dropped?
//				subgoal.drop();
//			}
//		}
		
//		System.out.println("lifecyle state: "+getId()+", "+lifecyclestate);
		
		this.lifecyclestate = lifecyclestate;
		
		if(PlanLifecycleState.PASSED.equals(lifecyclestate)
			|| PlanLifecycleState.FAILED.equals(lifecyclestate)
			|| PlanLifecycleState.ABORTED.equals(lifecyclestate))
		{
			//System.out.println("rplan finished: "+finished+" "+this);
			if(finished!=null)
				finished.setResult(null);
		}
	}
	
	/**
	 *  Get the reason.
	 *  @return The reason.
	 */
	public Object getReason()
	{
		return reason;
	}

//	/**
//	 *  Get the dispatchedelement.
//	 *  @return The dispatchedelement.
//	 */
//	public Object getDispatchedElement()
//	{
//		return dispatchedelement;
//	}
//
//	/**
//	 *  Set the dispatchedelement.
//	 *  @param dispatchedelement The dispatchedelement to set.
//	 */
//	public void setDispatchedElement(Object dispatchedelement)
//	{
//		this.dispatchedelement = dispatchedelement;
//	}
//	
	/**
	 *  Get the exception.
	 *  @return The exception.
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 *  Set the exception.
	 *  @param exception The exception to set.
	 */
	public void setException(Exception e)
	{
		this.exception = e;

		// Print exception, when relevant for user. 
		if(!(e instanceof GoalFailureException)
			&& !(e instanceof PlanFailureException))
		{
			System.err.println("Plan '"+getId()+"' threw exception: "+SUtil.getExceptionStacktrace(e));
		}
	}
	
	/**
	 *  Get the candidate.
	 *  @return The candidate.
	 */
	public ICandidateInfo getCandidate()
	{
		return candidate;
	}
//
//	/**
//	 *  Set the candidate.
//	 *  @param candidate The candidate to set.
//	 */
//	public void setCandidate(ICandidateInfo candidate)
//	{
//		this.candidate = candidate;
//	}

//	/**
//	 *  Test if the plan is waiting for a process element.
//	 */
//	public boolean isWaitingFor(Object procelem)
//	{
//		return RPlan.PlanProcessingState.WAITING.equals(getProcessingState()) 
//			&& waitabstraction!=null && waitabstraction.isWaitingFor(procelem);
//	}
//	
//	/**
//	 *  Get the waitabstraction.
//	 *  @return The waitabstraction.
//	 */
//	public WaitAbstraction getWaitAbstraction()
//	{
//		return waitabstraction;
//	}
//	
//	/**
//	 *  Set the waitabstraction.
//	 *  @param waitabstraction The waitabstraction to set.
//	 */
//	public void setWaitAbstraction(WaitAbstraction waitabstraction)
//	{
//		this.waitabstraction = waitabstraction;
//	}
//	
//	/**
//	 *  Test if the plan is always waiting for a process element (waitqueue wait).
//	 */
//	public boolean isWaitqueueWaitingFor(Object procelem)
//	{
//		// Do not dispatch process goals to waitqueue. (Hack? not allowed for v2, but would be easily possible for v3)
//		boolean	processgoal	= procelem instanceof RGoal && ((RGoal)procelem).getProcessingState()==GoalProcessingState.INPROCESS;
//		
//		return !processgoal && waitqueuewa!=null && waitqueuewa.isWaitingFor(procelem);
//	}
	
//	/**
//	 *  Get the waitabstraction.
//	 *  @return The waitabstraction.
//	 */
//	public WaitAbstraction getOrCreateWaitqueueWaitAbstraction()
//	{
//		if(waitqueuewa==null)
//			waitqueuewa = new WaitAbstraction();
//		return waitqueuewa;
//	}
//	
//	/**
//	 * 
//	 */
//	protected void addToWaitqueue(Object obj)
//	{
//		if(waitqueue==null)
//			waitqueue = new Waitqueue();
//		waitqueue.addElement(obj);
//	}
//	
//	/**
//	 * 
//	 */
//	public Object getFromWaitqueue(WaitAbstraction wa)
//	{
//		return waitqueue!=null ? waitqueue.getFromWaitqueue(wa) : null;
//	}
	
	/**
	 * 
	 */
	public boolean isPassed()
	{
		return RPlan.PlanLifecycleState.PASSED.equals(lifecyclestate);
	}
	
	/**
	 * 
	 */
	public boolean isFailed()
	{
		return RPlan.PlanLifecycleState.FAILED.equals(lifecyclestate);
	}
	
	/**
	 * 
	 */
	public boolean isAborted()
	{
		return RPlan.PlanLifecycleState.ABORTED.equals(lifecyclestate);
	}
	
	/**
	 *  Start the finishing of the plan.
	 */
	public boolean setFinishing()
	{
		boolean ret = false;
		if(!isFinishing())
		{
			finished = new Future<Void>();
			ret = true;
			
			if(isFinished())
			{
				finished.setResult(null);
			}
		}
		return ret;
	}
	
	/**
	 *  Test, if the plan end state (passed/failed/aborted) is started or done. 
	 */
	public boolean isFinishing()
	{
		return finished!=null;
	}
	
	/**
	 * 
	 */
	public boolean isFinished()
	{
		return isPassed() || isFailed() || isAborted();
	}
	
	/**
	 *  Add goal to current subgoals.
	 */
	public void addSubgoal(RGoal subgoal)
	{
		if(subgoals==null)
		{
			subgoals = new LinkedHashSet<>();
		}
		subgoals.add(subgoal);
	}
	
	/**
	 *  Remove goal from current subgoals.
	 */
	public void removeSubgoal(RGoal subgoal)
	{
		if(subgoals!=null)
		{
			subgoals.remove(subgoal);
		}
	}
	
	/**
	 * 
	 */
	public IFuture<Void> abort()
	{
		if(setFinishing())
		{
			if(!isFinished())
			{
//				// If plan is waiting interrupt waiting
//				if(PlanProcessingState.WAITING.equals(getProcessingState()))
//				{
//							// The resume command continues the blocked plan thread and
//							// the commands are to continue all listeners on hold
//							// This is not completely clean because the agent does not wait for these threads
//					
//							ICommand<ResumeCommandArgs> resc = getResumeCommand();
//							if(resc!=null)
//							{
//								//System.out.println("aborting5: "+this+", "+resc);
//								resc.execute(new ResumeCommandArgs(null, null, () -> new PlanAbortedException()));
//							}
//							List<ICommand<ResumeCommandArgs>> rescoms = getResumeCommands();
//							if(rescoms!=null)
//							{
//								ICommand<ResumeCommandArgs>[] tmp = (ICommand<ResumeCommandArgs>[])rescoms.toArray(new ICommand[rescoms.size()]);
//								//System.out.println("aborting6: "+this+", "+SUtil.arrayToString(tmp));
//								for(ICommand<ResumeCommandArgs> rescom: tmp)
//								{
//									rescom.execute(new ResumeCommandArgs(null, null, () -> new PlanAbortedException()));
//								}
//							}
//				}
				// Can be currently executing and being abort due to e.g. goal condition triggering
				/*else*/ if(!atomic /*&& PlanProcessingState.RUNNING.equals(getProcessingState())*/)
				{
					// abort immediately when running and not atomic -> otherwise later checks for aborted in endAtomic(). 
					throw new StepAborted();
					
					// if not running it will detect the abort in before/afterBlock() when next future.get() is
					// called or resumed and will avoid the next wait/wakeup
				}
			}			
		}
		
		return finished;
	}
	
	
//	/**
//	 *  Get the waitqueue.
//	 *  @return The waitqueue.
//	 */
//	public Waitqueue getWaitqueue()
//	{
//		if(waitqueue==null)
//		{
//			waitqueue = new Waitqueue();
//		}
//		return waitqueue;
//	}

	// methods that can be called from pojo plan

	/**
	 *  Wait for a delay.
	 */
	public IFuture<Void> waitFor(long delay)
	{
//		final Future<Void> ret = new Future<Void>();
//		
//		final ResumeCommand<Void> rescom = new ResumeCommand<Void>(ret, true);
//		addResumeCommand(rescom);

		return getComponent().getFeature(IExecutionFeature.class).waitForDelay(delay);
//			.then(v -> rescom.execute(null));
		
//		return ret;
	}
	
	/**
	 *  Dispatch a goal wait for its result.
	 */
	public <T, E> IFuture<E> dispatchSubgoal(final T goal)
	{
//		return dispatchSubgoal(goal, -1);
//	}
//	
//	/**
//	 *  Dispatch a goal wait for its result.
//	 */
//	public <T, E> IFuture<E> dispatchSubgoal(final T goal, long timeout)
//	{
//		final Future<E> ret = new Future<E>();
		
		// TODO: hack!!! goal from other capability needs different parents?
		final RGoal rgoal = new RGoal(goal, this, comp, getParentPojos());
		
//		final ResumeCommand<E> rescom = new ResumeCommand<E>(ret, false);
////		setResumeCommand(rescom);
//		addResumeCommand(rescom);
//		addTimer(timeout, rescom);
//
//		rgoal.addListener(new IResultListener<Void>()
//		{
//			public void resultAvailable(Void result)
//			{
//				if(getException()==null)
//				{
//					Object o = RGoal.getGoalResult(rgoal, ((MicroAgent)getAgent()).getClassLoader());
//					if(o==null)
//						o = goal;
//					setDispatchedElement(o);
//					
//					// Non-maintain goal -> remove subgoal.
//					if(rgoal.isFinished())
//					{
//						removeSubgoal(rgoal);
//					}
//					
//					// else keep maintain goal until plan is finished
//					// todo: allow explicit removal / redispatch
//				}
//				
//				rescom.execute(null);
//			}
//			
//			public void exceptionOccurred(Exception exception)
//			{
//				rescom.execute(new ResumeCommandArgs(null, null, () -> exception));
//				removeSubgoal(rgoal);
//			}
//		});
	
		addSubgoal(rgoal);
		
		IExecutionFeature.get().scheduleStep(new AdoptGoalAction(rgoal));
		
		@SuppressWarnings("unchecked")
		IFuture<E>	ret	= (IFuture<E>) rgoal.getFinished();
		return ret;
	}
//	
//	/**
//	 *  Wait for a fact change of a belief.
//	 */
//	public IFuture<ChangeInfo<?>> waitForFactChanged(String belname)
//	{
//		return waitForFactX(belname, new String[]{ChangeEvent.FACTCHANGED}, -1, null);
//	}
//	
//	/**
//	 *  Wait for a fact change of a belief.
//	 */
//	public IFuture<ChangeInfo<?>> waitForFactChanged(String belname , long timeout)
//	{
//		return waitForFactX(belname, new String[]{ChangeEvent.FACTCHANGED}, timeout, null);
//	}
//	
//	/**
//	 *  Wait for a fact being added to a belief.
//	 */
//	public IFuture<ChangeInfo<?>> waitForFactAdded(String belname)
//	{
//		return waitForFactX(belname, new String[]{ChangeEvent.FACTADDED}, -1, null);
//	}
//	
//	/**
//	 *  Wait for a fact being added to a belief.
//	 */
//	public IFuture<ChangeInfo<?>> waitForFactAdded(String belname, long timeout)
//	{
//		return waitForFactX(belname, new String[]{ChangeEvent.FACTADDED}, timeout, null);
//	}
//
//	/**
//	 *  Wait for a fact being removed from a belief.
//	 */
//	public IFuture<ChangeInfo<?>> waitForFactRemoved(String belname)
//	{
//		return waitForFactX(belname, new String[]{ChangeEvent.FACTREMOVED}, -1, null);
//	}
//	
//	/**
//	 *  Wait for a fact being removed from a belief.
//	 */
//	public IFuture<ChangeInfo<?>> waitForFactRemoved(String belname, long timeout)
//	{
//		return waitForFactX(belname, new String[]{ChangeEvent.FACTREMOVED}, timeout, null);
//	}
//	
//	/**
//	 *  Wait for a belief change.
//	 */
//	public IFuture<ChangeInfo<?>> waitForBeliefChanged(String belname)
//	{
//		return waitForFactX(belname, new String[]{ChangeEvent.BELIEFCHANGED}, -1, null);
//	}
//	
//	/**
//	 *  Wait for a belief change.
//	 */
//	public IFuture<ChangeInfo<?>> waitForBeliefChanged(String belname, long timeout)
//	{
//		return waitForFactX(belname, new String[]{ChangeEvent.BELIEFCHANGED}, timeout, null);
//	}
//	
//	/**
//	 *  Wait for a fact being added to a belief..
//	 */
//	public IFuture<ChangeInfo<?>> waitForFactX(String belname, String[] evtypes, long timeout, final IFilter<ChangeInfo<?>> filter)
//	{
//		Future<ChangeInfo<?>> ret = new Future<ChangeInfo<?>>();
//		
////		final BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)getAgent()).getInterpreter();
//				
//		// Also set waitabstraction to know what the plan is waiting for
//		final List<EventType> ets = new ArrayList<EventType>();
//		WaitAbstraction wa = new WaitAbstraction();
//		for(String evtype: evtypes)
//		{
//			EventType et = new EventType(evtype, belname);
//			wa.addChangeEventType(et);
//			ets.add(et);
//		}
////		setWaitAbstraction(wa);
//		
//		Object obj = getFromWaitqueue(wa);
//		if(obj!=null)
//		{
//			ret.setResult((ChangeInfo<?>)((ChangeEvent)obj).getValue());
////			ret = new Future<Object>(obj);
//		}
//		else
//		{
//			final String rulename = getRuleName();
//			
//			final ResumeCommand<ChangeInfo<?>> rescom = new ResumeCommand<ChangeInfo<?>>(ret, rulename, false);
////			final ResumeCommand<Object> rescom = new ResumeCommand<Object>(ret, rulename, false);
////			setResumeCommand(rescom);
//			addResumeCommand(rescom);
//			addTimer(timeout, rescom);
//					
//			Rule<Void> rule = new Rule<Void>(rulename, filter==null? ICondition.TRUE_CONDITION: new ICondition()
//			{
//				public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
//				{
//					return new Future<Tuple2<Boolean, Object>>(filter.filter((ChangeInfo<?>)event.getContent())? ICondition.TRUE: ICondition.FALSE);
//				}
//			}, new IAction<Void>()
//			{
//				public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
//				{
//					setDispatchedElement(new ChangeEvent(event));
//					rescom.execute(null);
//					return IFuture.DONE;
//				}
//			});
//					
//			rule.setEvents(ets);
//			getRuleSystem().getRulebase().addRule(rule);
//		}
//		
////		Future<ChangeInfo<?>> fut = new BDIFuture<ChangeInfo<?>>();
//		Future<ChangeInfo<?>> fut = new Future<ChangeInfo<?>>();
//		ret.addResultListener(new DelegationResultListener<ChangeInfo<?>>(fut)
//		{
//			public void customResultAvailable(ChangeInfo<?> result)
//			{
////				ChangeEvent ce = (ChangeEvent)result;
////				super.customResultAvailable(ce.getValue());
//				super.customResultAvailable(result);
//			}
//		});
//		
//		return fut;
//	}
//	
//	/**
//	 *  Wait for a fact being added or removed to a belief.
//	 */
//	public IFuture<ChangeInfo<?>> waitForFactAddedOrRemoved(String belname)
//	{
//		return waitForFactAddedOrRemoved(belname, -1);
//	}
	
//	/**
//	 *  Wait for a fact being added or removed to a belief.
//	 */
//	public IFuture<ChangeInfo<?>> waitForFactAddedOrRemoved(String belname, long timeout)
//	{
//		return waitForFactX(belname, new String[]{ChangeEvent.FACTADDED, ChangeEvent.FACTREMOVED}, timeout, null);		
//	}
	
//	/**
//	 *  Wait for a collection change.
//	 */
//	public <T> IFuture<ChangeInfo<T>> waitForCollectionChange(String belname, long timeout, IFilter<ChangeInfo<T>> filter)
//	{
//		// buahhh :-((( how to get this generics nightmare?
//		IFuture fut = waitForFactX(belname, new String[]{ChangeEvent.FACTCHANGED, ChangeEvent.FACTADDED, ChangeEvent.FACTREMOVED}, timeout, (IFilter)filter);
//		return (IFuture<ChangeInfo<T>>)fut;
//	}
	
//	/**
//	 *  Wait for a collection change.
//	 */
//	public <T> IFuture<ChangeInfo<T>> waitForCollectionChange(String belname, long timeout, final Object id)
//	{
//		IFuture fut = waitForFactX(belname, new String[]{ChangeEvent.FACTCHANGED, ChangeEvent.FACTADDED, ChangeEvent.FACTREMOVED}, timeout, new IFilter<ChangeInfo<?>>()
//		{
//			public boolean filter(ChangeInfo<?> info)
//			{
//				boolean ret = false;
//				if(info.getInfo()!=null)
//				{
//					ret = info.getInfo().equals(id);
//				}
//				return ret;
//			}
//		});
//		return (IFuture<ChangeInfo<T>>)fut;
//	}
//	
//	/**
//	 *  Wait for a condition.
//	 */
//	public IFuture<Void> waitForCondition(ICondition cond, String[] events)
//	{
//		return waitForCondition(cond, events, -1);
//	}
//	
//	/**
//	 *  Wait for a condition.
//	 */
//	public IFuture<Void> waitForCondition(final ICondition cond, final String[] events, long timeout)
//	{
////		final Future<E> ret = new BDIFuture<E>();
//		final Future<Void> ret = new Future<Void>();
//
////		final BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)getAgent()).getInterpreter();
//		
//		final String rulename = getRuleName();
//		
//		final ResumeCommand<Void> rescom = new ResumeCommand<Void>(ret, rulename, false);
////			setResumeCommand(rescom);
//		addResumeCommand(rescom);
//		addTimer(timeout, rescom);
//		
//		Rule<Void> rule = new Rule<Void>(rulename, cond!=null? cond: ICondition.TRUE_CONDITION, new IAction<Void>()
//		{
//			public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
//			{
//	//						if(rescom.equals(getResumeCommand()))
//	//						{
//					setDispatchedElement(new ChangeEvent(event));
//					rescom.execute(null);
//	//							RPlan.executePlan(RPlan.this, getAgent(), rescom);
//	//						}
//				return IFuture.DONE;
//			}
//		});
//		for(String ev: events)
//		{
//			rule.addEvent(new EventType(ev));
//		}
//		getRuleSystem().getRulebase().addRule(rule);
//		return ret;
//	}
//	
//	
//	public static class RescomTimer<T>	implements Runnable
//	{
//		ResumeCommand<T>	rescom;
//		boolean cancelled;
//		
//		public RescomTimer(ResumeCommand<T> rescom)
//		{
//			this.rescom	= rescom;
//		}
//		
//		public void	run()
//		{
//			if(!cancelled)
//			{
//				rescom.execute(new ResumeCommandArgs(null, null, () -> new TimeoutException()));
//			}
//		}
//		
//		public void	cancel()
//		{
//			// TODO support removal of timer entries in exe feature?
//			cancelled	= true;
//		}
//	}
//	
//	/**
//	 *  Add a timer to the resume command if timeout is set.
//	 */
//	public <T> void	addTimer(long timeout, ResumeCommand<T> rescom)
//	{
//		if(timeout>0)
//		{
//			RescomTimer<T>	ret =  new RescomTimer<>(rescom);
//			rescom.setTimer(ret);
//			IExecutionFeature.get().waitForDelay(timeout).then(v -> ret.run());
//		}
//	}
	
	/**
//	 * 
//	 * @return
//	 */
//	protected String getRuleName()
//	{
//		return getId()+"_wait_#"+cnt++;
//	}
//	
//	/**
//	 *  Called before blocking the component thread.
//	 */
//	public <T> void	beforeBlock(Future<T> fut)
//	{
//		testBodyAborted();
//		ISuspendable sus = ISuspendable.SUSPENDABLE.get();
//		if(sus!=null && !RPlan.PlanProcessingState.WAITING.equals(getProcessingState()))
//		{
//			final ResumeCommand<T> rescom = new ResumeCommand<T>(fut, sus, false);
//			setProcessingState(PlanProcessingState.WAITING);
////			System.out.println("setting rescom: "+getId()+" "+rescom);
//			resumecommand = rescom;
//		}
//	}
//	
//	/**
//	 *  Called after unblocking the component thread.
//	 */
//	public void	afterBlock()
//	{
//		testBodyAborted();
//		setProcessingState(PlanProcessingState.RUNNING);
//		setWaitAbstraction(null);
//		if(resumecommand!=null)
//		{
//			// performs only cleanup without setting future
////			System.out.println("afterblock rescom: "+getId()+" "+resumecommand);
//			resumecommand.execute(new ResumeCommandArgs(Boolean.FALSE, null, null));
////			resumecommand.execute(new Tuple2<Boolean, Boolean>(Boolean.FALSE, null));
//			resumecommand = null;
//		}
//	}

	/**
	 *  Check if plan is already aborted.
	 */
	protected void testBodyAborted()
	{
		// Throw error to exit body method of aborted plan.
		if(isFinishing() && PlanLifecycleState.BODY.equals(getLifecycleState()))
		{
			throw new StepAborted();
		}
	}

//	public record ResumeCommandArgs(Boolean donotify, Boolean abort, Supplier<Exception> exception) {}
//	
//	/**
//	 * 
//	 */
//	public class ResumeCommand<T> implements ICommand<ResumeCommandArgs>
//	{
//		protected ISuspendable sus;
//		protected Future<T> waitfuture;
//		protected String rulename;
//		protected RescomTimer<T> timer;
//		protected boolean isvoid;
//		
//		public ResumeCommand(Future<T> waitfuture, boolean isvoid)
//		{
//			this.waitfuture = waitfuture;
//			this.isvoid = isvoid;			
//		}
//		
//		public ResumeCommand(Future<T> waitfuture, String rulename, boolean isvoid)
//		{
//			this(waitfuture, isvoid);
////			System.out.println("created: "+this+" "+RPlan.this.getId());
//			this.rulename = rulename;
//		}
//		
//		public ResumeCommand(Future<T> waitfuture, ISuspendable sus, boolean isvoid)
//		{
//			this(waitfuture, isvoid);
//			this.sus = sus;
//		}
//		
//		public void setTimer(RescomTimer<T> timer)
//		{
//			this.timer = timer;
//		}
//
//		/**
//		 *  first Boolean: notify (default true)
//		 *  second Boolean: abort (default false)
//		 */
//		public void execute(ResumeCommandArgs args)
//		{
//			assert getAgent().getFeature(IExecutionFeature.class).isComponentThread();
//
////			System.out.println("exe: "+this+" "+RPlan.this.getId()+" "+this);
//
//			if(rulename!=null)
//			{
//				//System.out.println("rem rule: "+rulename);
////				BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
//				getRuleSystem().getRulebase().removeRule(rulename);
//			}
//			if(timer!=null)
//			{
//				timer.cancel();
//			}
//			waitabstraction = null;
//			
//			boolean notify = args!=null && args.donotify()!=null? args.donotify().booleanValue(): true;
//			boolean abort = args!=null && args.abort()!=null? args.abort().booleanValue(): sus!=null;
//			
//			if(notify && RPlan.PlanProcessingState.WAITING.equals(getProcessingState()))
//			{
//				boolean donotify = false;
//				if(resumecommands!=null && resumecommands.contains(this))
//				{
//					resumecommands.remove(this);
//					donotify = true;
//				}
//				if(this.equals(resumecommand))
//				{
////					System.out.println("clear rescom: "+getId());
//					resumecommand = null;
//					donotify = true;
//				}
//				
//				if(donotify)
//				{
//					/*if(!abort)//sus==null)
//					{*/
//						Exception ex	= args!=null && args.exception()!=null ? args.exception().get() : null;
//						if(ex!=null)
//						{
//							if(waitfuture instanceof ITerminableFuture)
//							{
//								//System.out.println("notify1: "+getId());
//								((ITerminableFuture<?>)waitfuture).terminate(ex);
//							}
//							else
//							{
//								//System.out.println("notify2: "+getId());
//								waitfuture.setExceptionIfUndone(ex);
//							}
//							
////							setException(null);	// Allow plan to continue when exception is catched.
//						}
//						else
//						{
//							Object o = getDispatchedElement();
//							if(o instanceof ChangeEvent)
//							{
//								o = ((ChangeEvent)o).getValue();
//							}
//							//System.out.println("notify3: "+getId());
//							waitfuture.setResultIfUndone(isvoid? null: (T)o);
//						}
//					/*}
//					else
//					{
//						System.out.println("notify4: "+getId());
//						waitfuture.abortGet(sus);
//					}*/
//				}
//			}
//		}
//
//		/**
//		 *  Get the waitfuture.
//		 *  @return The waitfuture
//		 */
//		public Future<T> getWaitfuture()
//		{
//			return waitfuture;
//		}
//	}
	
	/**
//	 * 
//	 */
//	public void addResumeCommand(ICommand<ResumeCommandArgs> rescom)
//	{
////		System.out.println("addResCom: "+this);
//		if(resumecommands==null)
//			resumecommands = new ArrayList<ICommand<ResumeCommandArgs>>();
//		resumecommands.add(rescom);
//	}
//	
//	/**
//	 * 
//	 */
//	public void removeResumeCommand(ICommand<Tuple2<Boolean, Boolean>> rescom)
//	{
//		if(resumecommands!=null)
//			resumecommands.remove(rescom);
//	}
//
//	/**
//	 *  Get the resumecommands.
//	 *  @return The resumecommands.
//	 */
//	public List<ICommand<ResumeCommandArgs>> getResumeCommands()
//	{
//		return resumecommands;
//	}
//	
//	/**
//	 *  Get the resumecommand.
//	 *  @return The resumecommand.
//	 */
//	public ICommand<ResumeCommandArgs> getResumeCommand()
//	{
//		return resumecommand;
//	}
//	
//	/**
//	 *  Get the result.
//	 *  @return The result.
//	 */
//	public Object getResult()
//	{
//		return result;
//	}
//
//	/**
//	 *  Set the result.
//	 *  @param result The result to set.
//	 */
//	public void setResult(Object result)
//	{
//		this.result = result;
//	}
//	
	
	/**
	 *  Check if currently inside Atomic block.
	 */
	@Override
	public boolean	isAtomic()
	{
		return atomic;
	}
	
	/**
	 *  When in atomic mode, plans will not be immediately aborted, e.g. when their goal succeeds or their context condition becomes false.
	 */
	@Override
	public void startAtomic()
	{
		if(atomic)
		{
			throw new IllegalStateException("Already in atomic block.");
		}
		this.atomic	= true;
	}

	/**
	 *  When not in atomic mode, plans will be immediately aborted, e.g. when their goal succeeds or their context condition becomes false.
	 */
	@Override
	public void endAtomic()
	{
		if(!atomic)
		{
			throw new IllegalStateException("Not in atomic block.");
		}
		this.atomic	= false;
		testBodyAborted();
	}

//	/**
//	 *  Set up a rule for the waitqueue to signal to what kinds of events this plan
//	 *  in principle reacts to.
//	 */
//	public void setupEventsRule(Collection<EventType> events)
//	{
//		final String rulename = getId()+"_waitqueue";
//		
//		Rule<Void> rule = new Rule<Void>(rulename, ICondition.TRUE_CONDITION, new IAction<Void>()
//		{
//			public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
//			{
//				System.out.println("Added to waitqueue: "+event);
//				addToWaitqueue(new ChangeEvent(event));
//				return IFuture.DONE;
//			}
//		});
//		rule.setEvents(events instanceof List ? (List<EventType>)events : new ArrayList<>(events));
//		IInternalBDIAgentFeature.get().getRuleSystem().getRulebase().updateRule(rule);
//	}
	
//	/**
//	 *  Test if element is succeeded.
//	 *  @return True, if is succeeded.
//	 */
//	public boolean	isSucceeded()
//	{
//		return isPassed();
//	}
//	
//	/**
//	 *  Check if the element is currently part of the agent's reasoning.
//	 *  E.g. the bases are always adopted and all of their contents such as goals, plans and beliefs.
//	 */
//	public boolean	isAdopted()
//	{
//		return true;
////	 	// Hack!!! Subgoals removed to late, TODO: fix hierarchic goal plan lifecycle management
////		System.out.println(this + " isAdopted(): "+(!(getReason() instanceof RParameterElement) || ((RParameterElement) getReason()).isAdopted()));
////		return !(getReason() instanceof RParameterElement) || ((RParameterElement) getReason()).isAdopted();
//	}
//	
	
	protected IPlanBody getBody()
	{
		return body;
	}
	
	/**
	 *  Set the pojo.
	 *  For class plans that need to be instantiated after rplan is created.
	 */	
	protected void	setPojo(Object pojoelement)
	{
		this.pojoelement	= pojoelement;
		
		// Re-generate list of all pojos on next access, i.e. now include pojoelement
		this.allpojos	= null;
	}

	/**
//	 *  Waitque holds events for later processing.
//	 */
//	public class Waitqueue
//	{
//		protected List<Object>	queue	= new ArrayList<Object>();
//		
//		public String toString()
//		{
//			return "Waitqueue("+RPlan.this+", "+queue.toString()+")";
//		}
//
//		public RPlan getPlan()
//		{
//			return RPlan.this;
//		}
//
//		public void addElement(Object element)
//		{
//			queue.add(element);
//		}
//		
//		/**
//		 *  Test if waitqueue is empty.
//		 */
//		public boolean isEmpty()
//		{
//			return queue.isEmpty();
//		}
//		
//		/**
//		 *  Get the currently contained elements of the waitqueue.
//		 *  @return The collected elements.
//		 */
//		public Object[] getElements()
//		{
//			return queue.toArray();
//		}
//
//		/**
//		 * 
//		 */
//		protected Object getFromWaitqueue(WaitAbstraction wa)
//		{
//			Object ret = null;
//			for(int i=0; i<queue.size(); i++)
//			{
//				Object obj = queue.get(i);
//				if(wa.isWaitingFor(obj))
//				{
//					ret = obj;
//					queue.remove(i);
//					break;
//				}
//			}
//			return ret;
//		}
//	}
}
