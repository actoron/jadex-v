package jadex.providedservice.impl;

import java.util.Collections;
import java.util.Map;

import jadex.bpmn.runtime.IBpmnComponentFeature;
import jadex.common.IValueFetcher;
import jadex.common.SUtil;
import jadex.core.impl.Component;
import jadex.javaparser.SJavaParser;
import jadex.providedservice.annotation.ProvideService;
import jadex.providedservice.impl.service.ProvidedServiceFeature;

public class BpmnProvidedServiceFeature	extends ProvidedServiceFeature
{
	protected BpmnProvidedServiceFeature(Component self)
	{
		super(self);
	}
	
	@Override
	public void init()
	{
		super.init();
		
		IBpmnComponentFeature	mf	= self.getFeature(IBpmnComponentFeature.class);
		ProvidedServiceModel	model	= loadModel();
		if(model!=null)
		{
			for(ProvidedServiceInfo info: model.getServices())
			{
				Object	service	= createServiceImplementation(info, self.getValueProvider());
				Class<?>	type	= info.getType().getType(((Component)self).getClassLoader(), mf.getModel().getAllImports());
				Map<Class<?>, ProvideService>	types	= Collections.singletonMap(type, null);
				addService(Collections.singletonList(self.getPojo()), service, info.getName(), types);
			}
		}
	}
	
	public ProvidedServiceModel loadModel()
	{
		IBpmnComponentFeature	mf	= self.getFeature(IBpmnComponentFeature.class);
		ProvidedServiceModel mymodel = (ProvidedServiceModel)BpmnProvidedServiceLoader.readFeatureModel(mf.getModel());		
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
				ser = SJavaParser.getParsedValue(impl, self.getFeature(IBpmnComponentFeature.class).getModel().getAllImports(), fetcher, self.getClass().getClassLoader());
//				System.out.println("added: "+ser+" "+model.getName());
		}
		else if(impl!=null && impl.getClazz()!=null)
		{
			if(impl.getClazz().getType(((Component)self).getClassLoader(), self.getFeature(IBpmnComponentFeature.class).getModel().getAllImports())!=null)
			{
				try
				{
					ser = impl.getClazz().getType(((Component)self).getClassLoader(), self.getFeature(IBpmnComponentFeature.class).getModel().getAllImports()).getConstructor().newInstance();
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
