package jadex.mj.feature.execution;

import jadex.mj.core.MjComponent;

public interface IMjExecutionFeature
{
	public static IMjExecutionFeature	of(MjComponent self)
	{
		return self.getFeature(IMjExecutionFeature.class);
	}
	
	public void scheduleStep(Runnable r);
}
