package jadex.featuretest.impl;

import java.util.function.Supplier;

import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.core.impl.IBootstrapping;
import jadex.featuretest.BootstrappingTest;
import jadex.featuretest.ITestLazyFeature;

public class TestLazyFeatureProvider extends FeatureProvider<ITestLazyFeature> implements ITestLazyFeature, IBootstrapping
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
	
	@Override
	public <T extends Component> T bootstrap(Class<T> type, Supplier<T> creator)
	{
		BootstrappingTest.bootstraps.add(getClass().getSimpleName()+"_beforeCreate");
		T	ret	= creator.get();
		BootstrappingTest.bootstraps.add(getClass().getSimpleName()+"_afterCreate");
		return ret;

	}
}
