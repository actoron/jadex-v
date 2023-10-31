package jadex.mj.publishservicejetty.impl;

import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.micro.MicroAgent;
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
