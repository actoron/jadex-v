package jadex.providedservice.impl;

import java.util.Set;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.model.IModelFeature;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.impl.service.ProvidedServiceFeatureProvider;

public class BpmnProvidedServiceFeatureProvider extends ComponentFeatureProvider<IProvidedServiceFeature> 
{
	@Override
	public Class<IProvidedServiceFeature> getFeatureType()
	{
		return IProvidedServiceFeature.class;
	}

	@Override
	public IProvidedServiceFeature createFeatureInstance(Component self)
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
	
	@Override
	public boolean replacesFeatureProvider(ComponentFeatureProvider<?> provider)
	{
		return provider instanceof ProvidedServiceFeatureProvider;
	}
}
