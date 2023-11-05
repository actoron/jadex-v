package jadex.bdiv3.runtime.impl;

import jadex.bdiv3.features.impl.BDIAgentFeature;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.execution.StepAborted;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.MicroAgent;

/**
 *  Abstract base class for plan body implementations.
 */
public abstract class AbstractPlanBody implements IPlanBody
{
	//-------- attributes --------
	
	/** The rplan. */
	protected RPlan rplan;
	
	/** The currently running plan part. */
	protected Future<Object>	partfuture;
	
	//-------- constructors --------
	
	/**
	 *  Create a new plan body.
	 */
	public AbstractPlanBody(RPlan rplan)
	{
		this.rplan = rplan;
	}

	//-------- methods --------
	
	/**
	 *  Get the body impl (object that is actually invoked).
	 *  @return The object representing the body. 
	 */
	public Object getBody()
	{
		return null;
	}
	
	/**
	 *  Execute the plan body.
	 */
	public IFuture<Void> executePlan()
	{
		final Future<Void> ret = new Future<Void>();
		
		int next; // next step 1: passed(), 2: failed(), 3: aborted()
		try
		{
			Object result	= internalInvokePart(0);
			if(rplan.getException()!=null)
			{
				// WTF!?
				System.err.println("Unthrown plan exception!?");
				System.err.println(SUtil.getExceptionStacktrace(rplan.getException()));
				throw rplan.getException();
			}
			
			// Automatically set goal result if goal has @GoalResult
			if(result!=null)
			{
				rplan.setResult(result);
				/*if(rplan.getReason() instanceof RServiceCall)
				{
					RServiceCall sc = (RServiceCall)rplan.getReason();
					InvocationInfo ii = sc.getInvocationInfo();
					ii.setResult(result);
				}
				else*/ if(rplan.getReason() instanceof RGoal)
				{
					RGoal rgoal = (RGoal)rplan.getReason();
					rgoal.setGoalResult(result, ((MicroAgent)getAgent()).getClassLoader(), null, rplan, null);
				}
			}
			// Next step: passed()
			next	= 1;
		}
		catch(Exception e)
		{
			if(rplan.getException()!=null && rplan.getException()!=e)
			{
				// WTF!?
				System.err.println("Duplicate plan exception!?");
				System.err.println(SUtil.getExceptionStacktrace(rplan.getException()));
				System.err.println(SUtil.getExceptionStacktrace(e));
			}
			rplan.setException(e);
			
			/*if(rplan.getReason() instanceof RServiceCall)
			{
				RServiceCall sc = (RServiceCall)rplan.getReason();
				InvocationInfo ii = sc.getInvocationInfo();
				ii.setResult(exception);
			}*/
			// Next step: failed() or aborted()
			next = e instanceof PlanAbortedException? 3: 2;
		}

		
		rplan.setFinishing();
		
		try
		{
			internalInvokePart(next);
		}
		catch(Exception e)
		{
			if(rplan.getException()==null)
			{
				rplan.setException(e);
				rplan.setLifecycleState(RPlan.PlanLifecycleState.FAILED);
			}
			// -> already failed/aborted
			else
			{
				// Print exception, otherwise it would be silently ignored.
				System.err.println("Warning: Exception in aborted() or failed() method: "+SUtil.getExceptionStacktrace(e));
			}
		}
		
		if(rplan.getException()==null)
		{
			rplan.setLifecycleState(RPlan.PlanLifecycleState.PASSED);
			ret.setResult(null);
		}
		else
		{
			rplan.setLifecycleState(rplan.getException() instanceof PlanAbortedException
				? RPlan.PlanLifecycleState.ABORTED : RPlan.PlanLifecycleState.FAILED);
			ret.setException(rplan.getException());
		}
		
		return ret;
	}
	
	/**
	 *  Issue abortion of the plan body, if currently running.
	 */
	public void abort()
	{
//		System.out.println("body.abort "+rplan);
		// TODO: plan suspendable to remember blocked futures of a specific plan
//		if(partfuture!=null)
//		{
//			Future<Object>	fut	= partfuture;
//			partfuture	= null;	// Needs to be set before to allow assert if null
//			fut.setExceptionIfUndone(new PlanAbortedException());
//		}
	}
	
	/**
	 *  Invoke a plan part.
	 */
	protected Object	internalInvokePart(int part)
	{		
		try
		{
			assert RPlan.RPLANS.get()==null : RPlan.RPLANS.get()+", "+rplan;
			RPlan.RPLANS.set(rplan);
			rplan.setProcessingState(RPlan.PlanProcessingState.RUNNING);
			Object res = null;
			if(part==0) 
			{
//				System.out.println("body of: "+rplan);
				rplan.setLifecycleState(RPlan.PlanLifecycleState.BODY);
				res = invokeBody(guessParameters(getBodyParameterTypes()));
			}
			else if(part==1)
			{
//				System.out.println("passed of: "+rplan);
//				rplan.setLifecycleState(RPlan.PlanLifecycleState.PASSING);
				res = invokePassed(guessParameters(getPassedParameterTypes()));
			}
			else if(part==2)
			{
//				System.out.println("failed of: "+rplan);
//				rplan.setLifecycleState(RPlan.PlanLifecycleState.FAILING);
				res = invokeFailed(guessParameters(getFailedParameterTypes()));
			}
			else if(part==3)
			{
//				System.out.println("aborted of: "+rplan);
//				rplan.setLifecycleState(RPlan.PlanLifecycleState.ABORTING);
				res = invokeAborted(guessParameters(getAbortedParameterTypes()));
			}
			
			if(res instanceof IFuture)
			{
				@SuppressWarnings("unchecked")
				IFuture<Object> fut = (IFuture<Object>)res;
//				// When future is not done set state to (non-blocking) waiting.
//				rplan.setProcessingState(PlanProcessingState.WAITING);
				res	= fut.get();
			}
			
			return res;
		}
//		catch(PlanFailureException e)
//		{
//			if(partfuture==ret)
//			{
//				partfuture	= null;
//			}
//			ret.setExceptionIfUndone(e);
//		}
//		catch(BodyAborted ba)
//		{
//			assert ret.isDone() && ret.getException() instanceof PlanAbortedException;
//		}
		catch(Throwable e)
		{
			// Print exception, when relevant for user. 
			if(!(e instanceof StepAborted)
				&& !(e instanceof PlanAbortedException)
				&& !(e instanceof PlanFailureException))
			{
				System.err.println("Plan '"+getRPlan().getModelElement().getName()+"' threw exception: "+SUtil.getExceptionStacktrace(e));
			}
			throw SUtil.throwUnchecked(e);
		}
		finally
		{
			assert RPlan.RPLANS.get()==rplan : RPlan.RPLANS.get()+", "+rplan;
			RPlan.RPLANS.set(null);
		}
	}
	
	/**
	 *  Invoke the plan body.
	 */
	public abstract Object invokeBody(Object[] params) throws BodyAborted;

	/**
	 *  Invoke the plan passed method.
	 */
	public abstract Object invokePassed(Object[] params);

	/**
	 *  Invoke the plan failed method.
	 */
	public abstract Object invokeFailed(Object[] params);

	/**
	 *  Invoke the plan aborted method.
	 */
	public abstract Object invokeAborted(Object[] params);
	
	/**
	 *  Get the body parameters.
	 */
	public abstract Class<?>[] getBodyParameterTypes();
	
	/**
	 *  Get the passed parameters.
	 */
	public abstract Class<?>[] getPassedParameterTypes();

	/**
	 *  Get the failed parameters.
	 */
	public abstract Class<?>[] getFailedParameterTypes();

	/**
	 *  Get the aborted parameters.
	 */
	public abstract Class<?>[] getAbortedParameterTypes();

	/**
	 *  Method that tries to guess the parameters for the method call.
	 */
	// Todo: parameter annotations (currently only required for event injection)
	public Object[] guessParameters(Class<?>[] ptypes)
	{
		if(ptypes==null)
			return null;
		
		return BDIAgentFeature.getInjectionValues(ptypes, null, rplan.getModelElement(), null, rplan, null);
	}

	/**
	 *  Get the rplan.
	 *  @return The rplan.
	 */
	public RPlan getRPlan()
	{
		return rplan;
	}

	/**
	 *  Get the agent.
	 *  @return The agent
	 */
	public IComponent getAgent()
	{
		return IExecutionFeature.get().getComponent();
	}
}
