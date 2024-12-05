package jadex.featuretest.impl;

import java.util.function.Supplier;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IBootstrapping;
import jadex.featuretest.BootstrappingTest;
import jadex.featuretest.ITestFeature2;

public class TestFeature2Provider extends ComponentFeatureProvider<ITestFeature2> implements ITestFeature2, IBootstrapping
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

	@Override
	public <T extends Component> T bootstrap(Class<T> type, Supplier<T> creator)
	{
		BootstrappingTest.bootstraps.add(getClass().getSimpleName()+"_beforeCreate");
		T	ret	= creator.get();
		BootstrappingTest.bootstraps.add(getClass().getSimpleName()+"_afterCreate");
		return ret;

	}
}
