package jadex.mj.requiredservice.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.providedservice.IMjProvidedServiceFeature;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.requiredservice.IMjRequiredServiceFeature;

public class MjRequiredServiceFeatureProvider extends MjFeatureProvider<IMjRequiredServiceFeature> 
{
	@Override
	public Class<IMjRequiredServiceFeature> getFeatureType()
	{
		return IMjRequiredServiceFeature.class;
	}

	@Override
	public IMjRequiredServiceFeature createFeatureInstance(MjComponent self)
	{
		return new MjRequiredServiceFeature(self);
	}
	
	@Override
	public Class<? extends MjComponent> getRequiredComponentType() 
	{
		return MjMicroAgent.class;
	}
}
