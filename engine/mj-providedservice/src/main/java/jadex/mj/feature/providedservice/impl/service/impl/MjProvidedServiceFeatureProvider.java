package jadex.mj.feature.providedservice.impl.service.impl;

import jadex.mj.core.impl.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.providedservice.IMjProvidedServiceFeature;
import jadex.mj.micro.MjMicroAgent;

public class MjProvidedServiceFeatureProvider extends MjFeatureProvider<IMjProvidedServiceFeature> 
{
	@Override
	public Class<IMjProvidedServiceFeature> getFeatureType()
	{
		return IMjProvidedServiceFeature.class;
	}

	@Override
	public IMjProvidedServiceFeature createFeatureInstance(MjComponent self)
	{
		return new MjProvidedServiceFeature(self);
	}
	
	@Override
	public Class<? extends MjComponent> getRequiredComponentType() 
	{
		return MjMicroAgent.class;
	}
}
