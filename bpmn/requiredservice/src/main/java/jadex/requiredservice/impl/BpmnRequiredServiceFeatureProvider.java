package jadex.requiredservice.impl;

import java.util.Set;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.requiredservice.IRequiredServiceFeature;

public class BpmnRequiredServiceFeatureProvider extends ComponentFeatureProvider<IBpmnRequiredServiceFeature> 
{
	@Override
	public Class<IBpmnRequiredServiceFeature> getFeatureType()
	{
		return IBpmnRequiredServiceFeature.class;
	}

	@Override
	public IBpmnRequiredServiceFeature createFeatureInstance(Component self)
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
}
