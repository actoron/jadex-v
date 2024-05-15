package jadex.nfproperty.impl;

import java.util.Set;

import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.micro.MicroAgent;
import jadex.nfproperty.INFPropertyFeature;
import jadex.requiredservice.IRequiredServiceFeature;


public class NFPropertyFeatureProvider extends FeatureProvider<INFPropertyFeature>
{	
	@Override
	public Class<INFPropertyFeature> getFeatureType()
	{
		return INFPropertyFeature.class;
	}

	@Override
	public INFPropertyFeature createFeatureInstance(Component self)
	{
		return new NFPropertyFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return MicroAgent.class;
	}
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		return Set.of(IRequiredServiceFeature.class);
	}
}

