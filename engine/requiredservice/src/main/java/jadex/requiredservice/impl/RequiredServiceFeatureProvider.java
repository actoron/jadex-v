package jadex.requiredservice.impl;

import java.util.Collections;
import java.util.Set;

import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.micro.MicroAgent;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.requiredservice.IRequiredServiceFeature;

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
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		return Set.of(IProvidedServiceFeature.class);
	}
}
