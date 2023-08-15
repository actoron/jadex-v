package jadex.mj.featuretest.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.lifecycle.IMjLifecycleFeature;
import jadex.mj.feature.lifecycle.impl.MjLifecycleFeatureProvider;
import jadex.mj.micro.MjMicroAgent;

public class MjTestLifecycleFeatureProvider extends MjFeatureProvider<IMjLifecycleFeature> implements IMjLifecycleFeature
{
	@Override
	public Class<IMjLifecycleFeature> getFeatureType()
	{
		return IMjLifecycleFeature.class;
	}
	
	@Override
	public Class<? extends MjComponent> getRequiredComponentType()
	{
		return MjMicroAgent.class;
	}

	@Override
	public IMjLifecycleFeature createFeatureInstance(MjComponent self)
	{
		return this;
	}
	
	@Override
	public boolean replacesFeatureProvider(MjFeatureProvider<IMjLifecycleFeature> provider)
	{
		return provider instanceof MjLifecycleFeatureProvider;
	}

	@Override
	public void terminate()
	{
		// ignore
	}
}
