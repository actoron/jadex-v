package jadex.nfproperty.impl;

import java.lang.System.Logger.Level;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadex.common.ClassInfo;
import jadex.common.MethodInfo;
import jadex.core.impl.ComponentManager;
import jadex.micro.MicroClassReader;
import jadex.model.annotation.NameValue;
import jadex.model.modelinfo.ModelInfo;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.nfproperty.annotation.SNameValue;
import jadex.nfproperty.impl.modelinfo.NFPropertyInfo;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.impl.service.BasicService;
import jadex.providedservice.impl.service.ProvidedServiceImplementation;
import jadex.providedservice.impl.service.ProvidedServiceInfo;
import jadex.providedservice.impl.service.ProvidedServiceModel;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.impl.RequiredServiceModel;

public class NFPropertyLoader 
{
	public static Object readFeatureModel(ModelInfo modelinfo, Class<?> clazz, ClassLoader cl)
	{
		Class<?> cma = clazz;
		
		NFPropertyModel model = new NFPropertyModel();
		boolean done = false;
		//boolean tagsdone = false;
		
		while(cma!=null && !cma.equals(Object.class))
		{
			// Take all, upper replace lower
			if(!done && MicroClassReader.isAnnotationPresent(cma, NFProperties.class, cl))
			{
				NFProperties val = (NFProperties)MicroClassReader.getAnnotation(cma, NFProperties.class, cl);
				
				List<NFPropertyInfo> props = createNFPropertyInfos(val);
				props.forEach(nfp -> model.addComponentProperty(nfp));
				
				// todo!
//				nfpropsdone = val.replace();
			}
			
			// Take all, upper replace lower
			/*if(!tagsdone && MicroClassReader.isAnnotationPresent(cma, Tags.class, cl))
			{
				Tags tags = (Tags)MicroClassReader.getAnnotation(cma, Tags.class, cl);
				
				NFPropertyInfo nfp = createNFPropertyInfo(tags);
				model.addComponentProperty(nfp);
				
				// todo!
//				tagsdone = val.replace();
			}*/
			
			cma = cma.getSuperclass();
		}
		
		// creation of required service nfps happens when required service proxy is created
		// they must be written toplevel - can be associated with reqser when name is same?!
		
		RequiredServiceModel rmodel = (RequiredServiceModel)modelinfo.getFeatureModel(IRequiredServiceFeature.class);
		//Map<String, RequiredServiceInfo> rsers = rmodel.getRequiredServices();
		//System.out.println("todo: required service nfprops");
		System.getLogger(NFPropertyLoader.class.getName()).log(Level.WARNING, "todo: required service nfprops");
		
//		RequiredServiceInfo[] rsis = new RequiredServiceInfo[reqs.length];
		/*for(int j=0; j<reqs.length; j++)
		{
			if(!configinfo.hasRequiredService(reqs[j].name()))
			{
				RequiredServiceBinding binding = createBinding(reqs[j]);
				List<NFRPropertyInfo> nfprops = createNFRProperties(reqs[j].nfprops());
				RequiredServiceInfo rsi = new RequiredServiceInfo(reqs[j].name(), reqs[j].type(), reqs[j].min(), reqs[j].max(),// reqs[j].multiple(), 
					binding, nfprops, Arrays.asList(reqs[j].tags()));
//					configinfo.setRequiredServices(rsis);
				configinfo.addRequiredService(rsi);
			}
		}*/
		
		// creation of provided service nfps happens
		
		ProvidedServiceModel pmodel = (ProvidedServiceModel)modelinfo.getFeatureModel(IProvidedServiceFeature.class);
		if(pmodel!=null)
		{
			ProvidedServiceInfo[] psis = pmodel.getServices();
			for(ProvidedServiceInfo psi: psis)
			{
				ProvidedServiceImplementation impl = psi.getImplementation();
				
				Class<?> implcl = null;
				if(impl.getClazz()!=null)
					implcl = impl.getClazz().getType(ComponentManager.get().getClassLoader());
				else if(impl.getClazz()==null && impl.getValue()==null)
					implcl = clazz; // pojoclass
				
				Map<MethodInfo, List<NFPropertyInfo>> serprops = createProvidedNFProperties(implcl, psi.getType().getType(ComponentManager.get().getClassLoader()));
			
				if(serprops.get(null)!=null)
				{
					serprops.get(null).forEach(p -> model.addProvidedServiceProperty(psi.getName(), p));
				}
				
				serprops.entrySet().forEach(entry ->
				{
					if(entry.getKey()!=null)
					{
						entry.getValue().forEach(p -> model.addProvidedServiceMethodProperty(psi.getName(), entry.getKey(), p));
					}
				});
			}
		}
				
		return model;
	}
	
	public static List<NFPropertyInfo> createNFPropertyInfos(NFProperties nfprops)
	{
		List<NFPropertyInfo> ret = new ArrayList<>();
		for(NFProperty prop: nfprops.value())
		{
			NameValue[] vals = prop.parameters();
			NFPropertyInfo nfp = new NFPropertyInfo(prop.name(), new ClassInfo(prop.value().getName()), SNameValue.createUnparsedExpressionsList(vals));
			ret.add(nfp);
		}
		return ret;
	}
	
	/*public static NFPropertyInfo createNFPropertyInfo(Tags tags)
	{		
		List<UnparsedExpression> params = new ArrayList<>();
//		if(val.argumentname().length()>0)
//			params.add(new UnparsedExpression(TagProperty.ARGUMENT, "\"val.argumentname()\""));

		for(int i=0; i<tags.value().length; i++)
		{
			Tag tag = tags.value()[i];
			
			//Object val = SJavaParser.evaluateExpression(tag.include(), agent.getModel().getAllImports(), getInternalAccess().getFetcher(), getInternalAccess().getClassLoader());
			//if(val instanceof Boolean && ((Boolean)val).booleanValue())
			//if(tag.include().length()>0)
			//	params.add(new UnparsedExpression(TagProperty.NAME+"_condition_"+i, tag.include()));
			params.add(new UnparsedExpression(TagProperty.NAME+"_"+i, tag.value()));
		}
		
		NFPropertyInfo nfp = new NFPropertyInfo(TagProperty.NAME, new ClassInfo(TagProperty.class), params);
		
		return nfp;
	}*/
	
	public static Map<MethodInfo, List<NFPropertyInfo>> createProvidedNFProperties(Class<?> impltype, Class<?> sertype)
	{
		Map<MethodInfo, List<NFPropertyInfo>> ret = new HashMap<MethodInfo, List<NFPropertyInfo>>();
		List<NFPropertyInfo> serprops = new ArrayList<NFPropertyInfo>();
		ret.put(null, serprops);
		
		List<Class<?>> classes = new ArrayList<Class<?>>();
		Class<?> superclazz = sertype;
		while(superclazz != null && !Object.class.equals(superclazz))
		{
			classes.add(superclazz);
			superclazz = superclazz.getSuperclass();
		}
		
		superclazz = impltype;
		while(superclazz != null && !BasicService.class.equals(superclazz) && !Object.class.equals(superclazz))
		{
			classes.add(superclazz);
			superclazz = superclazz.getSuperclass();
		}
		
		Map<MethodInfo, Method> meths = new HashMap<MethodInfo, Method>();
		for(Class<?> sclazz: classes)
		{
			if(sclazz.isAnnotationPresent(NFProperties.class))
			{
				List<NFPropertyInfo> nfps = createNFPropertyInfos(sclazz.getAnnotation(NFProperties.class));
				nfps.forEach(nfp -> serprops.add(nfp));
			}
			
			/*if(sclazz.isAnnotationPresent(Tags.class))
			{
				NFPropertyInfo nfp = createNFPropertyInfo(sclazz.getAnnotation(Tags.class));
				serprops.add(nfp);
			}*/
			
			Method[] methods = sclazz.getMethods();
			for(Method m : methods)
			{
				if(m.isAnnotationPresent(NFProperties.class))
				{
					MethodInfo mis = new MethodInfo(m.getName(), m.getParameterTypes());
					if(!meths.containsKey(mis))
					{
						meths.put(mis, m);
					}
				}
			}
		}
		
		for(MethodInfo key: meths.keySet())
		{
			List<NFPropertyInfo> nfps = createNFPropertyInfos(meths.get(key).getAnnotation(NFProperties.class));
			ret.put(key, nfps);
		}
		
		return ret;
	}
	

	
	
	
	/**
	 *  Create a required service info from annotation.
	 * /
	protected static RequiredServiceInfo createRequiredServiceInfo(RequiredService rs, ClassLoader cl)
	{
		RequiredServiceBinding binding = createBinding(rs);
		List<NFRPropertyInfo> nfprops = createNFRProperties(rs.nfprops());
		
		for(NFRProperty prop: rs.nfprops())
		{
			nfprops.add(new NFRPropertyInfo(prop.name(), new ClassInfo(prop.value().getName()), 
				new MethodInfo(prop.methodname(), prop.methodparametertypes())));
		}
		
		RequiredServiceInfo rsis = new RequiredServiceInfo(rs.name(), rs.type(), 
			rs.min(), rs.max(), binding, nfprops, Arrays.asList(rs.tags())); // rs.multiple()
		
		return rsis;
	}*/
	
	
	/**
	 *  Create req service props.
	 * /
	protected static List<NFRPropertyInfo> createNFRProperties(NFRProperty[] nfrp)
	{
		List<NFRPropertyInfo> nfprops = new ArrayList<NFRPropertyInfo>();
		for(NFRProperty prop: nfrp)
		{
			nfprops.add(new NFRPropertyInfo(prop.name(), new ClassInfo(prop.value().getName()), 
				new MethodInfo(prop.methodname(), prop.methodparametertypes())));
		}
		return nfprops;
	}*/
}
