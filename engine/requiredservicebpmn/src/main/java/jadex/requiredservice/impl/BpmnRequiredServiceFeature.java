package jadex.requiredservice.impl;

import jadex.core.impl.Component;
import jadex.model.IModelFeature;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;
import jadex.requiredservice.IRequiredServiceFeature;

public class BpmnRequiredServiceFeature extends RequiredServiceFeature
{
	protected BpmnRequiredServiceFeature(Component self)
	{
		super(self);
	}
	
	public RequiredServiceModel loadModel()
	{
		ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();
		final RequiredServiceModel mymodel = (RequiredServiceModel)BpmnRequiredServiceLoader.readFeatureModel(self.getFeature(IModelFeature.class).getModel());
		
		if(mymodel!=null)
		{
			AbstractModelLoader loader = AbstractModelLoader.getLoader((Class< ? extends Component>)self.getClass());
			loader.updateCachedModel(() ->
			{
				model.putFeatureModel(IRequiredServiceFeature.class, mymodel);
			});
		}
		
		return mymodel;
	}
}