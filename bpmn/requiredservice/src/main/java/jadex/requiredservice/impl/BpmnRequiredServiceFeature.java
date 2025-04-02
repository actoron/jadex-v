package jadex.requiredservice.impl;

import jadex.core.impl.Component;
import jadex.execution.impl.ILifecycle;
import jadex.future.IFuture;
import jadex.model.IModelFeature;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;
import jadex.requiredservice.IRequiredServiceFeature;

public class BpmnRequiredServiceFeature implements IBpmnRequiredServiceFeature, ILifecycle
{
	Component self;
	RequiredServiceModel	model;
	
	protected BpmnRequiredServiceFeature(Component self)
	{
		this.self	= self;
	}
	
	@Override
	public void onStart()
	{
		this.model	= loadModel();
	}
	
	@Override
	public void onEnd()
	{
		// NOP
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

	@Override
	public IFuture<Object> getService(String service)
	{
		RequiredServiceInfo	info	= getServiceInfo(service);
		Class<?>	type	= info.getType().getType(self.getClassLoader(), self.getFeature(IModelFeature.class).getModel().getAllImports());
		IRequiredServiceFeature	rsf	= self.getFeature(IRequiredServiceFeature.class);
		@SuppressWarnings("unchecked")
		IFuture<Object>	ret	= (IFuture<Object>)rsf.searchService(type);
		return ret;
	}

	@Override
	public RequiredServiceInfo getServiceInfo(String fservice)
	{
		return model.getService(fservice);
	}
}