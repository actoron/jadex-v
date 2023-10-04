package jadex.mj.publishservicejetty.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.publishservice.IMjPublishServiceFeature;

public class MjPublishServiceFeatureProvider extends MjFeatureProvider<IMjPublishServiceFeature> 
{
	@Override
	public Class<IMjPublishServiceFeature> getFeatureType()
	{
		return IMjPublishServiceFeature.class;
	}

	@Override
	public IMjPublishServiceFeature createFeatureInstance(MjComponent self)
	{
		return new MjPublishServiceJettyFeature(self);
	}
	
	@Override
	public Class<? extends MjComponent> getRequiredComponentType() 
	{
		return MjMicroAgent.class;
	}
}
