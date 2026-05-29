package jadex.featuretest.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.featuretest.ITestFeature2;

public class TestFeature2Provider extends ComponentFeatureProvider<ITestFeature2> implements ITestFeature2
{
	@Override
	public Class<ITestFeature2> getFeatureType()
	{
		return ITestFeature2.class;
	}

	@Override
	public ITestFeature2 createFeatureInstance(Component self)
	{
		return this;
	}
}
