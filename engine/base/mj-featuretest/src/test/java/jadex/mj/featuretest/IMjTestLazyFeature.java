package jadex.mj.featuretest;

import jadex.mj.core.MjComponent;

public interface IMjTestLazyFeature
{
	public static IMjTestLazyFeature	of(MjComponent self)
	{
		return self.getFeature(IMjTestLazyFeature.class);
	}
}
