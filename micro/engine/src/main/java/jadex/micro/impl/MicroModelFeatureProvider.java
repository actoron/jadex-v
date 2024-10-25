package jadex.micro.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.micro.MicroAgent;
import jadex.model.IModelFeature;

public class MicroModelFeatureProvider extends ComponentFeatureProvider<IModelFeature>
{
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return MicroAgent.class;
	}
	
	@Override
	public Class<IModelFeature> getFeatureType()
	{
		return IModelFeature.class;
	}

	@Override
	public IModelFeature createFeatureInstance(Component self)
	{
		return new MicroModelFeature((MicroAgent)self);
	}	
}
