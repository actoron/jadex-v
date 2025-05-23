package jadex.bdi.impl.plan;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.PlanAborted;
import jadex.bdi.impl.plan.RPlan.PlanLifecycleState;
import jadex.common.SUtil;
import jadex.future.IFuture;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.IValueFetcherCreator;

/**
 *  Plan represented by a class.
 */
public class ClassPlanBody implements IPlanBody
{
	/** The name of the plan class. */
	protected String	planname;
	
	/** Fetch local values, e.g. goal. */
	protected Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	contextfetchers;
	
	/** The plan precondition invocation handle. */
	protected IInjectionHandle	precondition;
	
	/** The plan context condition invocation handle. */
	protected IInjectionHandle	contextcondition;
	
	/** The plan constructor invocation handle. */
	protected IInjectionHandle	constructor;
	
	/** The plan body method invocation handle. */
	protected IInjectionHandle	body;
	
	/** The plan passed method invocation handle. */
	protected IInjectionHandle	passed;
	
	/** The plan failed method invocation handle. */
	protected IInjectionHandle	failed;
	
	/** The plan aborted method invocation handle. */
	protected IInjectionHandle	aborted;
	
	/**
	 *  Create a class plan body.
	 */
	public ClassPlanBody(String planname, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers,
		IInjectionHandle precondition, IInjectionHandle contextcondition, IInjectionHandle constructor,
		IInjectionHandle body, IInjectionHandle passed, IInjectionHandle failed, IInjectionHandle aborted)
	{
		this.planname	= planname;
		this.contextfetchers	= contextfetchers;
		this.precondition	= precondition;
		this.contextcondition	= contextcondition;
		this.constructor	= constructor;
		this.body	= body;
		this.passed	= passed;
		this.failed	= failed;
		this.aborted	= aborted;
	}
	
	@Override
	public boolean	hasPrecondition()
	{
		return precondition!=null;
	}
	
	@Override
	public boolean	checkPrecondition(RPlan rplan)
	{
		return checkCondition(rplan, precondition, "precondition");
	}
	
	@Override
	public boolean	checkContextCondition(RPlan rplan)
	{
		return checkCondition(rplan, contextcondition, "context condition");
	}
	
	/**
	 *  Create the pojo plan body if any (e.g. no pojo for method plan).
	 *  Skips pojo creation when already created.
	 */
	public void	createPojo(RPlan rplan)
	{
		if(rplan.getPojo()==null)
		{
			if(constructor!=null)
			{
				Object pojo	= internalInvokePart(rplan, constructor);
				rplan.setPojo(pojo);
			}
			else
			{
				throw new UnsupportedOperationException("No viable constructor for plan: "+planname);
			}
		}
	}
	
	@Override
	public void/*IFuture<?>*/ executePlan(RPlan rplan)
	{
//		final Future<Void> ret = new Future<Void>();
		
		PlanLifecycleState	next; // next step: call passed(), failed(), aborted()
		try
		{
			rplan.setLifecycleState(PlanLifecycleState.BODY);
			
			// Instantiate pojo if not already done (e.g. because of precondition evaluated in APL)
			createPojo(rplan);
			
			// Add rplan in try/catch because add plan executes injections, which may fail. 
			((BDIAgentFeature)rplan.getComponent().getFeature(IBDIAgentFeature.class)).addPlan(rplan, contextfetchers);
			
			// Call body
//			Object result	= 
				internalInvokePart(rplan, body);
			
			// Automatically set goal result if goal has @GoalResult
//			if(result!=null)
//			{
//				rplan.setResult(result);
//				/*if(rplan.getReason() instanceof RServiceCall)
//				{
//					RServiceCall sc = (RServiceCall)rplan.getReason();
//					InvocationInfo ii = sc.getInvocationInfo();
//					ii.setResult(result);
//				}
//				else*/ if(rplan.getReason() instanceof RGoal)
//				{
//					RGoal rgoal = (RGoal)rplan.getReason();
//					rgoal.setGoalResult(result, ((MicroAgent)getAgent()).getClassLoader(), null, rplan, null);
//				}
//			}
			// Next step: passed()
			next	= PlanLifecycleState.PASSING;
		}
		catch(PlanAborted e)
		{
			next = PlanLifecycleState.ABORTING;
		}
		catch(Exception e)
		{
			rplan.setException(e);
			next = PlanLifecycleState.FAILING;
		}

		
		rplan.setFinishing();
		
		try
		{
			rplan.setLifecycleState(next);
			
			// Only invoke next method when constructor didn't fail.
			if(rplan.getPojo()!=null)
			{
				IInjectionHandle	nexthandle	=
					next==PlanLifecycleState.PASSING ? passed
					: next==PlanLifecycleState.FAILING ? failed : aborted;
				internalInvokePart(rplan, nexthandle);
			}
			
			rplan.setLifecycleState(next==PlanLifecycleState.PASSING ? PlanLifecycleState.PASSED
					: next==PlanLifecycleState.FAILING ? PlanLifecycleState.FAILED
					: PlanLifecycleState.ABORTED);
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
		finally
		{
			((BDIAgentFeature)rplan.getComponent().getFeature(IBDIAgentFeature.class)).removePlan(rplan);
		}
		
//		if(rplan.getException()==null)
//		{
//			rplan.setLifecycleState(RPlan.PlanLifecycleState.PASSED);
//			ret.setResult(null);
//		}
//		else
//		{
//			rplan.setLifecycleState(rplan.getException() instanceof PlanAbortedException
//				? RPlan.PlanLifecycleState.ABORTED : RPlan.PlanLifecycleState.FAILED);
//			ret.setException(rplan.getException());
//		}
		
//		return ret;
	}
	
	/**
	 *  Execute a condition handle and get the boolean result.
	 */
	protected boolean	checkCondition(RPlan rplan, IInjectionHandle condition, String condname)
	{
		boolean	ret	= true;
		if(condition!=null)
		{
			Object	result	= null;
			// If currently inside (maybe other) plans -> reset RPLANS variable.
			RPlan	currentplan	= 	RPlan.RPLANS.get();
			RPlan.RPLANS.set(null);
			try
			{
				if(!condition.isStatic())
				{
					// TODO: add to agent so injections get executed?
					createPojo(rplan);
				}
			
				result	= internalInvokePart(rplan, condition);
			}
			catch(Exception e)
			{
				// Set exception to log error (hack!?)
				rplan.setException(e);
				ret	= false;
			}
			finally
			{
				RPlan.RPLANS.set(currentplan);
			}
			
			if(result instanceof Boolean)
			{
				ret	= (boolean)result;
			}
			else if(result!=null)
			{
				throw new UnsupportedOperationException("Plan "+condname+" must return a boolean value: "+rplan.getModelName()+", "+result);
			}
		}
		return ret;
	}

	/**
	 *  Invoke a plan part.
	 */
	protected static Object internalInvokePart(RPlan rplan, IInjectionHandle handle)
	{
		// Allow some methods to be null
		if(handle==null)
		{
			return null;
		}
		
		try
		{
			assert RPlan.RPLANS.get()==null : RPlan.RPLANS.get()+", "+rplan;
			RPlan.RPLANS.set(rplan);
//			rplan.setProcessingState(RPlan.PlanProcessingState.RUNNING);
			Object	res	= handle.apply(rplan.getComponent(), rplan.getAllPojos(), rplan, null);
			if(res instanceof IFuture)
			{
				res	= ((IFuture<?>)res).get();
			}
			
			return res;
		}
		finally
		{
			assert RPlan.RPLANS.get()==rplan : RPlan.RPLANS.get()+", "+rplan;
			RPlan.RPLANS.set(null);
		}
	}
}
