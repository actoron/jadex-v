package jadex.bpmn.runtime.impl;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.model.IModelFeature;

public class BpmnProcessModelFeatureProvider extends ComponentFeatureProvider<IModelFeature>
{
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return BpmnProcess.class;
	}
	
	@Override
	public Class<IModelFeature> getFeatureType()
	{
		return IModelFeature.class;
	}

	@Override
	public IModelFeature createFeatureInstance(Component self)
	{
		return new BpmnProcessModelFeature((BpmnProcess)self);
	}	
}
