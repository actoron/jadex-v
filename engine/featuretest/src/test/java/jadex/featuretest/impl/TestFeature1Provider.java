package jadex.featuretest.impl;

import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.featuretest.ITestFeature1;

public class TestFeature1Provider extends FeatureProvider<ITestFeature1> implements ITestFeature1
{
	@Override
	public Class<ITestFeature1> getFeatureType()
	{
		return ITestFeature1.class;
	}

	@Override
	public ITestFeature1 createFeatureInstance(Component self)
	{
		return this;
	}
}
