package jadex.providedservice.impl;

import jadex.common.IValueFetcher;
import jadex.core.impl.Component;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;
import jadex.model.IModelFeature;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.impl.service.AbstractServiceInvocationHandler;
import jadex.providedservice.impl.service.ProvidedServiceFeature;
import jadex.providedservice.impl.service.ProvidedServiceImplementation;
import jadex.providedservice.impl.service.ProvidedServiceInfo;
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
	public IFuture<Object> createServiceImplementation(ProvidedServiceInfo info, IValueFetcher fetcher) 
	{
		final Future<Object> ret = new Future<>();
		
		Object	ser	= null;
		ProvidedServiceImplementation impl = info.getImplementation();
		if(impl!=null && impl.getValue()!=null)
		{
			// todo: other Class imports, how can be found out?
			try
			{
//				SimpleValueFetcher fetcher = new SimpleValueFetcher(component.getFetcher());
//				fetcher.setValue("$servicename", info.getName());
//				fetcher.setValue("$servicetype", info.getType().getType(component.getClassLoader(), component.getModel().getAllImports()));
//				System.out.println("sertype: "+fetcher.fetchValue("$servicetype")+" "+info.getName());
				ser = SJavaParser.getParsedValue(impl, getComponent().getFeature(IModelFeature.class).getModel().getAllImports(), fetcher, getComponent().getClass().getClassLoader());
//				System.out.println("added: "+ser+" "+model.getName());
				ret.setResult(ser);
			}
			catch(RuntimeException e)
			{
//				e.printStackTrace();
				ret.setException(new RuntimeException("Service creation error: "+info, e));
			}
		}
		else if(impl!=null && impl.getClazz()!=null)
		{
			if(impl.getClazz().getType(getComponent().getClass().getClassLoader(), getComponent().getFeature(IModelFeature.class).getModel().getAllImports())!=null)
			{
				try
				{
					ser = impl.getClazz().getType(getComponent().getClass().getClassLoader(), getComponent().getFeature(IModelFeature.class).getModel().getAllImports()).newInstance();
					ret.setResult(ser);
				}
				catch(Exception e)
				{
					ret.setException(e);
				}
			}
			else
			{
				try
				{
					Class<?> c = Class.forName("jadex.extension.rs.publish.JettyRestPublishService", false, getComponent().getClass().getClassLoader());
					System.out.println("foundd: "+c);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				ret.setException(new RuntimeException("Could not load service implementation class: "+impl.getClazz()+" "+getComponent().getClass().getClassLoader()));
			}
		}
		else
		{
			ret.setResult(null);
		}
//		else if(IExternalAccess.class.equals(info.getType().getType(getComponent().getClassLoader())))
//		{
//			ser = getComponent().getExternalAccess();
//		}
		
		return ret;
	}
}
