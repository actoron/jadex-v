package jadex.providedservice.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.micro.MicroAgent;
import jadex.providedservice.IProvidedServiceFeature;

public class ProvidedServiceFeatureProvider extends ComponentFeatureProvider<IProvidedServiceFeature> 
{
	@Override
	public Class<IProvidedServiceFeature> getFeatureType()
	{
		return IProvidedServiceFeature.class;
	}

	@Override
	public IProvidedServiceFeature createFeatureInstance(Component self)
	{
		return new MicroProvidedServiceFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return MicroAgent.class;
	}
}
