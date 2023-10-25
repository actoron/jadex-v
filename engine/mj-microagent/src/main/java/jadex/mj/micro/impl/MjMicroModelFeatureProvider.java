package jadex.mj.micro.impl;

import jadex.mj.core.IMjModelFeature;
import jadex.mj.core.impl.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.micro.MjMicroAgent;

public class MjMicroModelFeatureProvider extends MjFeatureProvider<IMjModelFeature>
{
	@Override
	public Class< ? extends MjComponent> getRequiredComponentType()
	{
		return MjMicroAgent.class;
	}
	
	@Override
	public Class<IMjModelFeature> getFeatureType()
	{
		return IMjModelFeature.class;
	}

	@Override
	public IMjModelFeature createFeatureInstance(MjComponent self)
	{
		return new MjMicroModelFeature((MjMicroAgent)self);
	}	
}
