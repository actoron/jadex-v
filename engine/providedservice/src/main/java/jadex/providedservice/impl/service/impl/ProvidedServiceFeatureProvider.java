package jadex.providedservice.impl.service.impl;

import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.micro.MicroAgent;
import jadex.providedservice.IProvidedServiceFeature;

public class ProvidedServiceFeatureProvider extends FeatureProvider<IProvidedServiceFeature> 
{
	@Override
	public Class<IProvidedServiceFeature> getFeatureType()
	{
		return IProvidedServiceFeature.class;
	}

	@Override
	public IProvidedServiceFeature createFeatureInstance(Component self)
	{
		return new ProvidedServiceFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return MicroAgent.class;
	}
}
