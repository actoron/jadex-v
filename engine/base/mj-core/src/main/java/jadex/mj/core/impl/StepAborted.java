package jadex.mj.core.impl;

import java.util.UUID;

/**
 *  An error thrown to abort the thread execution of a blocked component step.
 */
public class StepAborted extends ThreadDeath 
{
	UUID	cid;
	
	public StepAborted(UUID cid)
	{
		this.cid	= cid;
	}
	
	@Override
	public String toString()
	{
		return super.toString()+"("+cid+")";
	}
}
