package jadex.bdi.runtime.impl;

import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;

public class BDIAgentFeatureProvider extends ComponentFeatureProvider<IBDIAgentFeature>
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
	
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return BDIAgent.class;
	}
}
