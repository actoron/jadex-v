package jadex.mj.featuretest.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.featuretest.IMjTestFeature2;

public class MjTestFeature2Provider extends MjFeatureProvider<IMjTestFeature2> implements IMjTestFeature2
{
	@Override
	public Class<IMjTestFeature2> getFeatureType()
	{
		return IMjTestFeature2.class;
	}

	@Override
	public IMjTestFeature2 createFeatureInstance(MjComponent self)
	{
		return this;
	}
}
