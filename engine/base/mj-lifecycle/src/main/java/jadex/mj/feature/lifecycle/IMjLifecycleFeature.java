package jadex.mj.feature.lifecycle;

import jadex.mj.core.MjComponent;

public interface IMjLifecycleFeature
{
	public static IMjLifecycleFeature	of(MjComponent self)
	{
		return self.getFeature(IMjLifecycleFeature.class);
	}

	public void terminate();
}
