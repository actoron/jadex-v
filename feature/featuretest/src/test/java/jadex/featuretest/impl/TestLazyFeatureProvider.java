package jadex.featuretest.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.featuretest.ITestLazyFeature;

public class TestLazyFeatureProvider extends ComponentFeatureProvider<ITestLazyFeature> implements ITestLazyFeature
{
	@Override
	public Class<ITestLazyFeature> getFeatureType()
	{
		return ITestLazyFeature.class;
	}

	@Override
	public ITestLazyFeature createFeatureInstance(Component self)
	{
		return this;
	}
	
	@Override
	public boolean isLazyFeature()
	{
		return true;
	}
}
