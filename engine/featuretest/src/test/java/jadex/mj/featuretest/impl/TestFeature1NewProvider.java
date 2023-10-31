package jadex.mj.featuretest.impl;

import jadex.mj.core.impl.Component;
import jadex.mj.core.impl.FeatureProvider;
import jadex.mj.featuretest.ITestFeature1;

public class TestFeature1NewProvider extends FeatureProvider<ITestFeature1> implements ITestFeature1
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
	public boolean replacesFeatureProvider(FeatureProvider<ITestFeature1> provider)
	{
		return provider.getClass().equals(TestFeature1Provider.class);
	}
	
	@Override
	public String toString()
	{
		// TODO Auto-generated method stub
		return super.toString();
	}
}
