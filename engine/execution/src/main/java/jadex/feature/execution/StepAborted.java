package jadex.feature.execution;

import jadex.core.ComponentIdentifier;

/**
 *  An error thrown to indicate the abortion of a blocked component step.
 */
@SuppressWarnings("serial")
public class StepAborted extends Error 
{
	ComponentIdentifier	cid;
	
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
