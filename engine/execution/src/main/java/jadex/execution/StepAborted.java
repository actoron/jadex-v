package jadex.execution;

import jadex.core.ComponentIdentifier;

/**
 *  An error thrown to indicate the abortion of a blocked component step.
 */
@SuppressWarnings("serial")
public class StepAborted extends Error 
{
	ComponentIdentifier	cid;
	
	public StepAborted()
	{
		this(IExecutionFeature.isAnyComponentThread() ?
			IExecutionFeature.get().getComponent().getId(): null);
	}
	
	public StepAborted(ComponentIdentifier cid)
	{
		this.cid	= cid;
	}
	
	@Override
	public String toString()
	{
		return super.toString()+"("+cid+")";
	}
}
