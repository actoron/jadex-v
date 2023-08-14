package jadex.mj.featuretest.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.lifecycle.IMjLifecycleFeature;

public class MjTestLifecycleFeatureProvider extends MjFeatureProvider<IMjLifecycleFeature> implements IMjLifecycleFeature
{
	@Override
	public Class<IMjLifecycleFeature> getFeatureType()
	{
		return IMjLifecycleFeature.class;
	}

	@Override
	public IMjLifecycleFeature createFeatureInstance(MjComponent self)
	{
		return this;
	}

	@Override
	public void terminate()
	{
		// ignore
	}
}
