package jadex.mj.featuretest.impl;

import java.util.function.Supplier;

import jadex.mj.core.impl.IBootstrapping;
import jadex.mj.core.impl.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.featuretest.BootstrappingTest;
import jadex.mj.featuretest.IMjTestFeature2;

public class MjTestFeature2Provider extends MjFeatureProvider<IMjTestFeature2> implements IMjTestFeature2, IBootstrapping
{
	@Override
	public Class<IMjTestFeature2> getFeatureType()
	{
		return IMjTestFeature2.class;
	}

	@Override
	public IMjTestFeature2 createFeatureInstance(MjComponent self)
	{
		return this;
	}

	@Override
	public <T extends MjComponent> T bootstrap(Class<T> type, Supplier<T> creator)
	{
		BootstrappingTest.bootstraps.add(getClass().getSimpleName()+"_beforeCreate");
		T	ret	= creator.get();
		BootstrappingTest.bootstraps.add(getClass().getSimpleName()+"_afterCreate");
		return ret;

	}
}
