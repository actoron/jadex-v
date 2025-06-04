package jadex.publishservicejetty.impl;

import java.util.Set;

import jadex.core.impl.Component;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.impl.PublishServiceFeatureProvider;
import jadex.requiredservice.IRequiredServiceFeature;

public class PublishJettyFeatureProvider extends PublishServiceFeatureProvider
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
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		return Set.of(IProvidedServiceFeature.class, IRequiredServiceFeature.class);
	}
}
