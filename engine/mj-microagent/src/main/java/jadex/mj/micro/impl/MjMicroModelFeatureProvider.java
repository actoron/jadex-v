package jadex.mj.micro.impl;

import jadex.mj.core.impl.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.model.IMjModelFeature;

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
