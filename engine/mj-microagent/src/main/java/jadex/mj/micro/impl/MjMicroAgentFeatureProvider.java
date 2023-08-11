package jadex.mj.micro.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.micro.MjMicroAgent;

public class MjMicroAgentFeatureProvider extends MjFeatureProvider<MjMicroAgentFeature>
{
	@Override
	public Class< ? extends MjComponent> getRequiredComponentType()
	{
		return MjMicroAgent.class;
	}
	
	@Override
	public Class<MjMicroAgentFeature> getFeatureType()
	{
		return MjMicroAgentFeature.class;
	}

	@Override
	public MjMicroAgentFeature createFeatureInstance(MjComponent self)
	{
		return new MjMicroAgentFeature((MjMicroAgent)self);
	}
}
