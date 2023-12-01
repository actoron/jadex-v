package jadex.bpmn.runtime;

import jadex.bpmn.features.IBpmnComponentFeature;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;

public class BpmnProcessFeatureProvider extends FeatureProvider<IBpmnComponentFeature>
{
	@Override
	public IBpmnComponentFeature createFeatureInstance(Component self)
	{
		return new BpmnProcessFeature((BpmnProcess)self);
	}
	
	@Override
	public Class<IBpmnComponentFeature> getFeatureType()
	{
		return IBpmnComponentFeature.class;
	}
}
