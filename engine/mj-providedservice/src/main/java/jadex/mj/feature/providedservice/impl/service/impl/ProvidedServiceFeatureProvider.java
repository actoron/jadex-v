package jadex.mj.feature.providedservice.impl.service.impl;

import jadex.mj.core.impl.Component;
import jadex.mj.core.impl.FeatureProvider;
import jadex.mj.feature.providedservice.IProvidedServiceFeature;
import jadex.mj.micro.MicroAgent;

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
