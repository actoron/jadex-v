package jadex.providedservice.impl;

import java.util.Collections;
import java.util.Map;

import jadex.common.IValueFetcher;
import jadex.common.SUtil;
import jadex.core.impl.Component;
import jadex.execution.impl.ILifecycle;
import jadex.javaparser.SJavaParser;
import jadex.model.IModelFeature;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.annotation.ProvideService;
import jadex.providedservice.impl.service.ProvidedServiceFeature;

public class BpmnProvidedServiceFeature	implements IBpmnProvidedServiceFeature, ILifecycle
{
	protected Component	self;
	
	protected BpmnProvidedServiceFeature(Component self)
	{
		this.self	= self;
	}
	
	@Override
	public void onStart()
	{
		IModelFeature	mf	= self.getFeature(IModelFeature.class);
		ProvidedServiceFeature	psf	= (ProvidedServiceFeature) self.getFeature(IProvidedServiceFeature.class);

		ProvidedServiceModel	model	= loadModel();
		for(ProvidedServiceInfo info: model.getServices())
		{
			Object	service	= createServiceImplementation(info, self.getValueProvider());
			Class<?>	type	= info.getType().getType(self.getClassLoader(), mf.getModel().getAllImports());
			Map<Class<?>, ProvideService>	types	= Collections.singletonMap(type, null);
			psf.addService(service, info.getName(), types);
		}
	}
	
	@Override
	public void onEnd()
	{
		// NOP -> services are removed by basic provided service feature automatically 
	}
	
	public ProvidedServiceModel loadModel()
	{
		IModelFeature	mf	= self.getFeature(IModelFeature.class);
		ProvidedServiceModel mymodel = (ProvidedServiceModel)BpmnProvidedServiceLoader.readFeatureModel(mf.getModel());
		
		if(mymodel!=null)
		{
			AbstractModelLoader loader = AbstractModelLoader.getLoader((Class<? extends Component>)self.getClass());
			loader.updateCachedModel(() ->
			{
				ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();
				model.putFeatureModel(IProvidedServiceFeature.class, mymodel);
			});
		}
		
		return mymodel;
	}
	
	/**
	 *  Create a service implementation from description.
	 */
	public Object	createServiceImplementation(ProvidedServiceInfo info, IValueFetcher fetcher) 
	{
		Object	ser	= null;
		ProvidedServiceImplementation impl = info.getImplementation();
		if(impl!=null && impl.getValue()!=null)
		{
			// todo: other Class imports, how can be found out?
//				SimpleValueFetcher fetcher = new SimpleValueFetcher(component.getFetcher());
//				fetcher.setValue("$servicename", info.getName());
//				fetcher.setValue("$servicetype", info.getType().getType(component.getClassLoader(), component.getModel().getAllImports()));
//				System.out.println("sertype: "+fetcher.fetchValue("$servicetype")+" "+info.getName());
				ser = SJavaParser.getParsedValue(impl, self.getFeature(IModelFeature.class).getModel().getAllImports(), fetcher, self.getClass().getClassLoader());
//				System.out.println("added: "+ser+" "+model.getName());
		}
		else if(impl!=null && impl.getClazz()!=null)
		{
			if(impl.getClazz().getType(self.getClassLoader(), self.getFeature(IModelFeature.class).getModel().getAllImports())!=null)
			{
				try
				{
					ser = impl.getClazz().getType(self.getClassLoader(), self.getFeature(IModelFeature.class).getModel().getAllImports()).getConstructor().newInstance();
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
			
			// !!??!?
//			else
//			{
//				try
//				{
//					Class<?> c = Class.forName("jadex.extension.rs.publish.JettyRestPublishService", false, self.getClass().getClassLoader());
//					System.out.println("foundd: "+c);
//				}
//				catch(Exception e)
//				{
//					e.printStackTrace();
//				}
//				ret.setException(new RuntimeException("Could not load service implementation class: "+impl.getClazz()+" "+self.getClass().getClassLoader()));
//			}
		}
//		else
//		{
//			ret.setResult(null);
//		}
//		else if(IExternalAccess.class.equals(info.getType().getType(getComponent().getClassLoader())))
//		{
//			ser = getComponent().getExternalAccess();
//		}
		
		return ser;
	}
}
