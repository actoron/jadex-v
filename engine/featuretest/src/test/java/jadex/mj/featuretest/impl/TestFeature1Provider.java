package jadex.mj.featuretest.impl;

import jadex.mj.core.impl.Component;
import jadex.mj.core.impl.FeatureProvider;
import jadex.mj.featuretest.ITestFeature1;

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
