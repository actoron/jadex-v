package jadex.bpmn.runtime;

import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.model.IModelFeature;

public class BpmnProcessModelFeatureProvider extends FeatureProvider<IModelFeature>
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
