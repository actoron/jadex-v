package jadex.feature.execution.impl;

/**
 *  Additional methods of execution feature implementations only to be used by non-user code.
 */
public interface IInternalExecutionFeature
{
	/**
	 *  Add a step listener.
	 */
	public void	addStepListener(IStepListener lis);
	
	/**
	 *  Remove a step listener.
	 */
	public void	removeStepListener(IStepListener lis);
	
	/**
	 *  Terminate the feature.
	 */
	public void terminate();
}
