package jadex.requiredservice.impl;

import jadex.core.impl.Component;
import jadex.micro.MicroAgent;
import jadex.model.IModelFeature;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;
import jadex.requiredservice.IRequiredServiceFeature;

public class MicroRequiredServiceFeature extends RequiredServiceFeature
{
	protected MicroRequiredServiceFeature(Component self)
	{
		super(self);
	}
	
	public RequiredServiceModel loadModel()
	{
		ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();

		RequiredServiceModel mymodel = (RequiredServiceModel)model.getFeatureModel(IRequiredServiceFeature.class);
		if(mymodel==null)
		{
			mymodel = (RequiredServiceModel)RequiredServiceLoader.readFeatureModel(((MicroAgent)self).getPojo().getClass(), this.getClass().getClassLoader());
			final RequiredServiceModel fmymodel = mymodel;
			AbstractModelLoader loader = AbstractModelLoader.getLoader((Class< ? extends Component>)self.getClass());
			loader.updateCachedModel(() ->
			{
				model.putFeatureModel(IRequiredServiceFeature.class, fmymodel);
			});
		}
		
		return mymodel;
	}
}