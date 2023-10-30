package jadex.mj.publishservicejetty.impl;

import jadex.mj.core.impl.Component;
import jadex.mj.core.impl.FeatureProvider;
import jadex.mj.micro.MicroAgent;
import jadex.mj.publishservice.IPublishServiceFeature;

public class PublishServiceFeatureProvider extends FeatureProvider<IPublishServiceFeature> 
{
	@Override
	public Class<IPublishServiceFeature> getFeatureType()
	{
		return IPublishServiceFeature.class;
	}

	@Override
	public IPublishServiceFeature createFeatureInstance(Component self)
	{
		return new PublishServiceJettyFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return MicroAgent.class;
	}
}
