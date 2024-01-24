package jadex.requiredservice.impl;

import java.util.Set;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.requiredservice.IRequiredServiceFeature;

public class BpmnRequiredServiceFeatureProvider extends FeatureProvider<IRequiredServiceFeature> 
{
	@Override
	public Class<IRequiredServiceFeature> getFeatureType()
	{
		return IRequiredServiceFeature.class;
	}

	@Override
	public IRequiredServiceFeature createFeatureInstance(Component self)
	{
		return new BpmnRequiredServiceFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return BpmnProcess.class;
	}
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		return Set.of(IProvidedServiceFeature.class);
	}
	
	@Override
	public boolean replacesFeatureProvider(FeatureProvider<IRequiredServiceFeature> provider) 
	{
		return provider instanceof RequiredServiceFeatureProvider;
	}
}
