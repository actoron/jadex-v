package jadex.mj.featuretest.impl;

import java.util.function.Supplier;

import jadex.mj.core.impl.IBootstrapping;
import jadex.mj.core.impl.Component;
import jadex.mj.core.impl.FeatureProvider;
import jadex.mj.featuretest.BootstrappingTest;
import jadex.mj.featuretest.ITestLazyFeature;

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
