package jadex.mj.requiredservice.impl;

import jadex.mj.core.impl.Component;
import jadex.mj.core.impl.FeatureProvider;
import jadex.mj.feature.providedservice.IProvidedServiceFeature;
import jadex.mj.micro.MicroAgent;
import jadex.mj.requiredservice.IRequiredServiceFeature;

public class RequiredServiceFeatureProvider extends FeatureProvider<IRequiredServiceFeature> 
{
	@Override
	public Class<IRequiredServiceFeature> getFeatureType()
	{
		return IRequiredServiceFeature.class;
	}

	@Override
	public IRequiredServiceFeature createFeatureInstance(Component self)
	{
		return new RequiredServiceFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return MicroAgent.class;
	}
}
