package jadex.mj.feature.lifecycle.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.lifecycle.IMjLifecycleFeature;

public class MjLifecycleFeatureProvider extends MjFeatureProvider<IMjLifecycleFeature>
{
	@Override
	public Class<IMjLifecycleFeature> getFeatureType()
	{
		return IMjLifecycleFeature.class;
	}

	@Override
	public IMjLifecycleFeature createFeatureInstance(MjComponent self)
	{
		return new MjLifecycleFeature(self);
	}
}
