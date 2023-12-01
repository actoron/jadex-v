package jadex.bdi.runtime.impl;

import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;

public class BDIAgentFeatureProvider extends FeatureProvider<IBDIAgentFeature>
{
	@Override
	public IBDIAgentFeature createFeatureInstance(Component self)
	{
		return new BDIAgentFeature((BDIAgent)self);
	}
	
	@Override
	public Class<IBDIAgentFeature> getFeatureType()
	{
		return IBDIAgentFeature.class;
	}
}
