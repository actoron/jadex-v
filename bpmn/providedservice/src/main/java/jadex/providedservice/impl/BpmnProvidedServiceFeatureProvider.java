package jadex.providedservice.impl;

import java.util.Set;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.model.IModelFeature;

public class BpmnProvidedServiceFeatureProvider extends ComponentFeatureProvider<IBpmnProvidedServiceFeature> 
{
	@Override
	public Class<IBpmnProvidedServiceFeature> getFeatureType()
	{
		return IBpmnProvidedServiceFeature.class;
	}

	@Override
	public IBpmnProvidedServiceFeature createFeatureInstance(Component self)
	{
		return new BpmnProvidedServiceFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return BpmnProcess.class;
	}
	
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		return Set.of(IModelFeature.class);
	}
}
