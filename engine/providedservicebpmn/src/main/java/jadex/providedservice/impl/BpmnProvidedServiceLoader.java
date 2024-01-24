package jadex.providedservice.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.model.MSubProcess;
import jadex.common.ClassInfo;
import jadex.common.UnparsedExpression;
import jadex.javaparser.SJavaParser;
import jadex.model.modelinfo.IModelInfo;
import jadex.providedservice.impl.service.AbstractServiceInvocationHandler;
import jadex.providedservice.impl.service.ProvidedServiceImplementation;
import jadex.providedservice.impl.service.ProvidedServiceInfo;
import jadex.providedservice.impl.service.ProvidedServiceModel;

public class BpmnProvidedServiceLoader 
{
	public static Object readFeatureModel(IModelInfo modelinfo)
	{
		MBpmnModel model = (MBpmnModel)modelinfo.getRawModel();
		ProvidedServiceModel ret = null;
	
		ClassLoader cl = BpmnProvidedServiceLoader.class.getClassLoader();
		
		Map<String, Object> exts = model.getExtension("providedservice");
		
		if(exts!=null)
		{
			ret = new ProvidedServiceModel();

			for(String name: exts.keySet())
			{
				Map<String, String> attrs = (Map<String, String>)exts.get(name); 
				//String name = attrs.get("name");
				ClassInfo itrface = attrs.get("interface") != null? new ClassInfo(attrs.get("interface")) : null;
				ClassInfo clazz = attrs.get("class") != null? new ClassInfo(attrs.get("class")) : null;
				String proxytype = attrs.get("proxytype");
				String impl = attrs.get("implementation");
				
				ProvidedServiceInfo ps = new ProvidedServiceInfo();
				ps.setName(name);
				ps.setType(itrface);
				ps.setImplementation(new ProvidedServiceImplementation());
				ps.getImplementation().setClazz(clazz);
				ps.getImplementation().setProxytype(proxytype);
				ps.getImplementation().setValue(impl);
				ret.addService(ps);
			}
		}
		
		final Map<MSubProcess, List<MActivity>> evtsubstarts = model.getEventSubProcessStartEventMapping();
		if(evtsubstarts!=null)
		{
			//ProvidedServiceInfo[] psis = modelinfo.getProvidedServices();
			ProvidedServiceInfo[] psis = ret!=null? ret.getServices(): null;
			Set<Class<?>> haspsis = new HashSet<Class<?>>();
			if(psis!=null)
			{
				for(ProvidedServiceInfo psi: psis)
				{
					haspsis.add(psi.getType().getType(cl));
				}
			}
			
			for(Map.Entry<MSubProcess, List<MActivity>> entry: evtsubstarts.entrySet())
			{
				Class<?> iface = null;
				
				List<MActivity> macts = entry.getValue();
				for(MActivity mact: macts)
				{
					if(MBpmnModel.EVENT_START_MESSAGE.equals(mact.getActivityType()))
					{
						if(mact.hasPropertyValue(MActivity.IFACE))
						{
							if(iface==null)
							{
								UnparsedExpression uexp = mact.getPropertyValue(MActivity.IFACE);
								iface = (Class<?>)SJavaParser.parseExpression(uexp, modelinfo.getAllImports(), cl).getValue(null);
							}
							
							if(iface!=null && !haspsis.contains(iface))
							{
								// found interface without provided service impl
								break;
							}
						}
					}
				}
				
				// todo: provided service scope
				if(iface!=null && !haspsis.contains(iface))
				{
					String exp = "java.lang.reflect.Proxy.newProxyInstance($component.getClassLoader()," 
						+ "new Class[]{"+iface.getName()+".class"
						+ "}, new jadex.providedservice.impl.ProcessServiceInvocationHandler($component, \""+entry.getKey().getId()+"\"))";
					ProvidedServiceImplementation psim = new ProvidedServiceImplementation(null, exp, AbstractServiceInvocationHandler.PROXYTYPE_DECOUPLED, null);
					ProvidedServiceInfo psi = new ProvidedServiceInfo("internal_"+iface.getName(), iface, psim);
					//modelinfo.addProvidedService(psi);
					
					if(ret==null)
						ret = new ProvidedServiceModel();
					ret.addService(psi);
				}
			}
		}
		
		return ret;
	}	
}
