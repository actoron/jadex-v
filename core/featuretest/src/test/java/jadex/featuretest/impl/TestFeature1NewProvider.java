package jadex.featuretest.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.featuretest.ITestFeature1;

public class TestFeature1NewProvider extends ComponentFeatureProvider<ITestFeature1> implements ITestFeature1
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
	
	@Override
	public boolean replacesFeatureProvider(ComponentFeatureProvider<ITestFeature1> provider)
	{
		return provider.getClass().equals(TestFeature1Provider.class);
	}
}
