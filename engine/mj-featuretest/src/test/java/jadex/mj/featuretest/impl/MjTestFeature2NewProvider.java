package jadex.mj.featuretest.impl;

import java.util.function.Supplier;

import jadex.mj.core.impl.IBootstrapping;
import jadex.mj.core.impl.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.featuretest.BootstrappingTest;
import jadex.mj.featuretest.IMjTestFeature2;
import jadex.mj.micro.MjMicroAgent;

public class MjTestFeature2NewProvider extends MjFeatureProvider<IMjTestFeature2> implements IMjTestFeature2, IBootstrapping
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
	public Class< ? extends MjComponent> getRequiredComponentType()
	{
		return MjMicroAgent.class;
	}
	
	@Override
	public boolean replacesFeatureProvider(MjFeatureProvider<IMjTestFeature2> provider)
	{
		return provider.getClass().equals(MjTestFeature2Provider.class);
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
