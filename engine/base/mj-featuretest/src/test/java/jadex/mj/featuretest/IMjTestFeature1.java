package jadex.mj.featuretest;

import jadex.mj.core.MjComponent;

public interface IMjTestFeature1
{
	public static IMjTestFeature1	of(MjComponent self)
	{
		return self.getFeature(IMjTestFeature1.class);
	}
}
