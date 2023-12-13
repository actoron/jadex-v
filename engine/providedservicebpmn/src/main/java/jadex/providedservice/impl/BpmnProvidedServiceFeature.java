package jadex.providedservice.impl;

import jadex.core.impl.Component;
import jadex.model.IModelFeature;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.impl.service.ProvidedServiceFeature;
import jadex.providedservice.impl.service.ProvidedServiceModel;

public class BpmnProvidedServiceFeature	extends ProvidedServiceFeature
{	
	protected BpmnProvidedServiceFeature(Component self)
	{
		super(self);
	}
	
	protected ProvidedServiceModel loadModel()
	{
		final ProvidedServiceModel mymodel = (ProvidedServiceModel)BpmnProvidedServiceLoader.readFeatureModel(self.getFeature(IModelFeature.class).getModel());
		
		if(mymodel!=null)
		{
			AbstractModelLoader loader = AbstractModelLoader.getLoader((Class< ? extends Component>)self.getClass());
			loader.updateCachedModel(() ->
			{
				ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();
				model.putFeatureModel(IProvidedServiceFeature.class, mymodel);
			});
		}
		
		return mymodel;
	}
}
