package jadex.bdi.impl.plan;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.plan.RPlan.PlanLifecycleState;
import jadex.execution.StepAborted;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.IValueFetcherCreator;

/**
 *  Plan represented by a single method.
 */
public class MethodPlanBody implements IPlanBody
{
	/** Fetch local values, e.g. goal. */
	protected Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	contextfetchers;
	
	/** The plan body method invocation handle. */
	protected IInjectionHandle	body;
	
	/**
	 *  Create a method plan body.
	 */
	public MethodPlanBody(Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers, IInjectionHandle body)
	{
		this.contextfetchers	= contextfetchers;
		this.body	= body;
	}
	
	@Override
	public void	executePlan(RPlan rplan)
	{
		try
		{
			rplan.setLifecycleState(PlanLifecycleState.BODY);
			
			// Add rplan in try/catch because add plan executes injections, which may fail. 
			((BDIAgentFeature)rplan.getComponent().getFeature(IBDIAgentFeature.class)).addPlan(rplan, contextfetchers);
			
			ClassPlanBody.internalInvokePart(rplan, body);
			
			rplan.setLifecycleState(PlanLifecycleState.PASSED);
		}
		catch(Exception e)
		{
			rplan.setLifecycleState(PlanLifecycleState.FAILED);
			rplan.setException(e);
		}
		catch(StepAborted e)
		{
			rplan.setLifecycleState(PlanLifecycleState.ABORTED);
			throw e;
		}
		finally
		{
			((BDIAgentFeature)rplan.getComponent().getFeature(IBDIAgentFeature.class)).removePlan(rplan, contextfetchers);			
		}
	}
}
