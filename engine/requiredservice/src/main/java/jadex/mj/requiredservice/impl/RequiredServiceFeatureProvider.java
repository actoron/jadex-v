package jadex.mj.requiredservice.impl;

import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.micro.MicroAgent;
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
