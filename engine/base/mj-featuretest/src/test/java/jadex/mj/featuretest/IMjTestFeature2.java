package jadex.mj.featuretest;

import jadex.mj.core.impl.MjComponent;

public interface IMjTestFeature2
{
	public static IMjTestFeature2	of(MjComponent self)
	{
		return self.getFeature(IMjTestFeature2.class);
	}
}
