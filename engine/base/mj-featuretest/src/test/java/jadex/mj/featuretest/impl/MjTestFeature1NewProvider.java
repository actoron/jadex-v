package jadex.mj.featuretest.impl;

import jadex.mj.core.impl.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.featuretest.IMjTestFeature1;

public class MjTestFeature1NewProvider extends MjFeatureProvider<IMjTestFeature1> implements IMjTestFeature1
{
	@Override
	public Class<IMjTestFeature1> getFeatureType()
	{
		return IMjTestFeature1.class;
	}

	@Override
	public IMjTestFeature1 createFeatureInstance(MjComponent self)
	{
		return this;
	}
	
	@Override
	public boolean replacesFeatureProvider(MjFeatureProvider<IMjTestFeature1> provider)
	{
		return provider.getClass().equals(MjTestFeature1Provider.class);
	}
	
	@Override
	public String toString()
	{
		// TODO Auto-generated method stub
		return super.toString();
	}
}
