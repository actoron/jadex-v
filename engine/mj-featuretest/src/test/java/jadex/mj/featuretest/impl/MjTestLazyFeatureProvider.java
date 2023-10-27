package jadex.mj.featuretest.impl;

import java.util.function.Supplier;

import jadex.mj.core.impl.IBootstrapping;
import jadex.mj.core.impl.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.featuretest.BootstrappingTest;
import jadex.mj.featuretest.IMjTestLazyFeature;

public class MjTestLazyFeatureProvider extends MjFeatureProvider<IMjTestLazyFeature> implements IMjTestLazyFeature, IBootstrapping
{
	@Override
	public Class<IMjTestLazyFeature> getFeatureType()
	{
		return IMjTestLazyFeature.class;
	}

	@Override
	public IMjTestLazyFeature createFeatureInstance(MjComponent self)
	{
		return this;
	}
	
	@Override
	public boolean isLazyFeature()
	{
		return true;
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
