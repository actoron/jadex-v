package jadex.bdiv3.features.impl;

import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;

public class BDIAgentFeatureProvider	extends FeatureProvider<IBDIAgentFeature>
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
