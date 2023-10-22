package jadex.mj.featuretest.impl;

import jadex.mj.core.impl.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.featuretest.IMjTestFeature1;

public class MjTestFeature1Provider extends MjFeatureProvider<IMjTestFeature1> implements IMjTestFeature1
{
	@Override
	public Class<IMjTestFeature1> getFeatureType()
	{
		return IMjTestFeature1.class;
	}

	@Override
	public IMjTestFeature1 createFeatureInstance(MjComponent self)
	{
		return this;
	}
}
