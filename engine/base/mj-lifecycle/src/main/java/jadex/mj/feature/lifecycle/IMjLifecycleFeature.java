package jadex.mj.feature.lifecycle;

import jadex.mj.feature.execution.IMjExecutionFeature;

public interface IMjLifecycleFeature
{
	public static IMjLifecycleFeature	get()
	{
		return IMjExecutionFeature.get().getComponent().getFeature(IMjLifecycleFeature.class);
	}

	public void terminate();
}
