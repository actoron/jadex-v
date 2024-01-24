package jadex.providedservice.impl;

import java.util.Set;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.IBpmnComponentFeature;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.micro.MicroAgent;
import jadex.model.IModelFeature;
import jadex.providedservice.IProvidedServiceFeature;

public class BpmnProvidedServiceFeatureProvider extends FeatureProvider<IProvidedServiceFeature> 
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
}
