package jadex.mj.feature.execution.impl;

/**
 *  Additional methods of execution feature implementations only to be used by non-user code.
 */
public interface IMjInternalExecutionFeature
{
	/**
	 *  Add a step listener.
	 */
	public void	addStepListener(IStepListener lis);
	
	/**
	 *  Remove a step listener.
	 */
	public void	removeStepListener(IStepListener lis);
}
