package jadex.mj.featuretest.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.featuretest.IMjTestLazyFeature;

public class MjTestLazyFeatureProvider extends MjFeatureProvider<IMjTestLazyFeature> implements IMjTestLazyFeature
{
	@Override
	public Class<IMjTestLazyFeature> getFeatureType()
	{
		return IMjTestLazyFeature.class;
	}

	@Override
	public IMjTestLazyFeature createFeatureInstance(MjComponent self)
	{
		return this;
	}
	
	@Override
	public boolean isLazyFeature()
	{
		return true;
	}
}
