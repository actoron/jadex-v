package jadex.bdi.impl.plan;

import jadex.bdi.impl.plan.RPlan.PlanLifecycleState;
import jadex.execution.StepAborted;
import jadex.injection.impl.IInjectionHandle;

/**
 *  Plan represented by a single method.
 */
public class MethodPlanBody implements IPlanBody
{
	/** The plan body method invocation handle. */
	protected IInjectionHandle	body;
	
	/**
	 *  Create a method plan body.
	 */
	public MethodPlanBody(IInjectionHandle body)
	{
		this.body	= body;
	}
	
	@Override
	public void	executePlan(RPlan rplan)
	{
		try
		{
			rplan.setLifecycleState(PlanLifecycleState.BODY);
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
	}
}
