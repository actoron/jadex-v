package jadex.mj.featuretest;

import jadex.mj.core.impl.MjComponent;

public interface IMjTestLazyFeature
{
	public static IMjTestLazyFeature	of(MjComponent self)
	{
		return self.getFeature(IMjTestLazyFeature.class);
	}
}
