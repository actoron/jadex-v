package jadex.publishservicejetty.impl;

import java.util.Set;

import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.micro.MicroAgent;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.publishservice.IPublishServiceFeature;
import jadex.requiredservice.IRequiredServiceFeature;

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
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		return Set.of(IProvidedServiceFeature.class, IRequiredServiceFeature.class);
	}
}
