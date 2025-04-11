package jadex.bdi.impl.plan;

import jadex.future.Future;
import jadex.future.IFuture;
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
	public IFuture<?> executePlan(RPlan rplan)
	{
		// TODO: set passed/failed and execute passed/failed/aborted
		// TODO: log exceptions
		try
		{
			Object	ret	= body.apply(rplan.getComponent(), rplan.getParentPojos(), rplan);
			if(ret!=null && !(ret instanceof IFuture))
			{
				throw new UnsupportedOperationException("Plan methods must return IFuture or null: "+this);
			}
			return (IFuture<?>)ret;
		}
		catch(Exception e)
		{
			rplan.setException(e);
			return new Future<Object>(e);
		}
	}
}
