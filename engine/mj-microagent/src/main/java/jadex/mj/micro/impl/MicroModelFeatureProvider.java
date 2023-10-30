package jadex.mj.micro.impl;

import jadex.mj.core.impl.Component;
import jadex.mj.core.impl.FeatureProvider;
import jadex.mj.micro.MicroAgent;
import jadex.mj.model.IModelFeature;

public class MicroModelFeatureProvider extends FeatureProvider<IModelFeature>
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
