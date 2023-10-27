package jadex.bdiv3.features.impl;

import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.micro.impl.MjMicroAgentFeature;

/**
 *  Internal interface of the bdi lifecycle feature.
 */
public interface IInternalBDILifecycleFeature
{
	public static IInternalBDILifecycleFeature	get()
	{
		return (IInternalBDILifecycleFeature)IMjExecutionFeature.get().getComponent().getFeature(MjMicroAgentFeature.class);
	}
	
	/**
	 *  Get the inited.
	 *  @return The inited.
	 */
	public boolean isInited();
	
	/**
	 *  Set the inited state.
	 *  @param The inited state.
	 */
	public void setInited(boolean inited);
	
	/**
	 *  Get the shutdown.
	 *  @return The shutdown.
	 */
	public boolean isShutdown();
	
	/**
	 *  Set the shutdown state.
	 *  @param The shutdown state.
	 */
	public void setShutdown(boolean shutdown);
}
