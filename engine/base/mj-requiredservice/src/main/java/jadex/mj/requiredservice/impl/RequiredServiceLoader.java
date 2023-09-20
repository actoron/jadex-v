package jadex.mj.requiredservice.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import jadex.common.ClassInfo;
import jadex.common.FieldInfo;
import jadex.common.MethodInfo;
import jadex.common.UnparsedExpression;
import jadex.mj.feature.providedservice.ServiceScope;
import jadex.mj.micro.MicroClassReader;
import jadex.mj.requiredservice.RequiredServiceBinding;
import jadex.mj.requiredservice.RequiredServiceInfo;
import jadex.mj.requiredservice.annotation.OnService;
import jadex.mj.requiredservice.annotation.RequiredService;
import jadex.mj.requiredservice.annotation.RequiredServices;

public class RequiredServiceLoader 
{
	public static Object readFeatureModel(final Class<?> clazz, ClassLoader cl)
	{
		Class<?> cma = clazz;
		
		//Map<String, RequiredServiceInfo> rservices = new HashMap<>();
		RequiredServiceModel rsm = new RequiredServiceModel();
		boolean reqsdone = false;
		
		while(cma!=null && !cma.equals(Object.class))
		{
			// Take all but new overrides old
			if(!reqsdone)
			{
				if(MicroClassReader.isAnnotationPresent(cma, RequiredServices.class, cl))
				{
					RequiredServices val = (RequiredServices)MicroClassReader.getAnnotation(cma, RequiredServices.class, cl);
					RequiredService[] vals = val.value();
					reqsdone = val.replace();
					
					for(int i=0; i<vals.length; i++)
					{
						RequiredServiceInfo rsis = createRequiredServiceInfo(vals[i], cl);
					
						checkAndAddRequiredServiceInfo(rsis, rsm.getRequiredServices(), cl);
	
	//					if(rsers.containsKey(vals[i].name()))
	//					{
	//						RequiredServiceInfo old = (RequiredServiceInfo)rsers.get(vals[i].name());
	//						if(old.getMin()!=rsis.getMin() || old.getM()!=rsis.getMax() || !old.getType().getType(cl).equals(rsis.getType().getType(cl)))
	//							throw new RuntimeException("Extension hierarchy contains incompatible required service more than once: "+vals[i].name());
	//					}
	//					else
	//					{
	//						rsers.put(vals[i].name(), rsis);
	//					}
					}
				}
				
				// Find injection targets by reflection (agent, arguments, services)
				Field[] fields = cma.getDeclaredFields();
				
				for(int i=0; i<fields.length; i++)
				{
					if(MicroClassReader.isAnnotationPresent(fields[i], OnService.class, cl))
					{
						//if("secser".equals(fields[i].getName()))
						//	System.out.println("secser");
						
						OnService ser = MicroClassReader.getAnnotation(fields[i], OnService.class, cl);
						RequiredService rs = ser.requiredservice();
	
						RequiredServiceInfo rsis = createRequiredServiceInfo(rs, cl);
						String name = rs.name().length()>0? rs.name(): fields[i].getName();
						
						if(Object.class.equals(rsis.getType().getType(cl)))
							rsis.setType(new ClassInfo(fields[i].getType()));
								
						if(ser.name().length()>0)
							checkAndAddRequiredServiceInfo(rsis, rsm.getRequiredServices(), cl);
						
						ServiceInjectionInfo sii = new ServiceInjectionInfo().setFieldInfo(new FieldInfo(fields[i])).setRequiredServiceInfo(rsis);
						
						if(ser.query().toBoolean()!=null)
							sii.setQuery(ser.query().toBoolean());
						else
							sii.setQuery(false); // default on fields is false
						sii.setRequired(ser.required().toBoolean());
						sii.setLazy(ser.required().toBoolean());
						sii.setActive(ser.active());
						
						rsm.addServiceInjection(name, sii);
					}
				}
				
				// Find method injection targets by reflection (services)
				Method[] methods = cma.getDeclaredMethods();
				for(int i=0; i<methods.length; i++)
				{
					if(MicroClassReader.isAnnotationPresent(methods[i], OnService.class, cl))
					{
						OnService ser = MicroClassReader.getAnnotation(methods[i], OnService.class, cl);
						RequiredService rs = ser.requiredservice();
							
						//String name = ser.requiredservice().name().length()>0? ser.requiredservice().name(): guessName(methods[i].getName());
						String name = ser.name();
						
						if(name.length()==0)
							name = rs.name();
						if(name.length()==0)
							MicroClassReader.guessName(methods[i].getName());
						
						RequiredServiceInfo rsis = (RequiredServiceInfo)rsm.getRequiredServices().get(name);
						if(rsis==null)
						{
							rsis = createRequiredServiceInfo(rs, cl);
							
							if(new ClassInfo(Object.class).equals(rsis.getType()))
							{
								Class<?> iftype = Object.class.equals(ser.requiredservice().type())? MjRequiredServiceFeature.guessParameterType(methods[i].getParameterTypes(), cl): ser.requiredservice().type();
								rsis.setType(new ClassInfo(iftype));
							}
							
							checkAndAddRequiredServiceInfo(rsis, rsm.getRequiredServices(), cl);
						}
										
						ServiceInjectionInfo sii = new ServiceInjectionInfo().setMethodInfo(new MethodInfo(methods[i])).setRequiredServiceInfo(rsis);
								
						//if(ser.requiredservice().name().length()>0)
						//	checkAndAddRequiredServiceInfo(rsis, rsers, cl);
						
						if(ser.query().toBoolean()!=null)
							sii.setQuery(ser.query().toBoolean());
						else
							sii.setQuery(true); // default on methods is true
						sii.setRequired(ser.required().toBoolean());
						sii.setLazy(ser.required().toBoolean());
						sii.setActive(ser.active());
						
						rsm.addServiceInjection(name, sii);
					}
				}
			}
			cma = cma.getSuperclass();
		}
		
		return rsm;//.values().toArray(new Object[rservices.size()]);
	}
	
	/**
	 *  Create a required service info from annotation.
	 */
	protected static RequiredServiceInfo createRequiredServiceInfo(RequiredService rs, ClassLoader cl)
	{
		RequiredServiceBinding binding = createBinding(rs);
		//List<NFRPropertyInfo> nfprops = null;//createNFRProperties(rs.nfprops());
		
		/*for(NFRProperty prop: rs.nfprops())
		{
			nfprops.add(new NFRPropertyInfo(prop.name(), new ClassInfo(prop.value().getName()), 
				new MethodInfo(prop.methodname(), prop.methodparametertypes())));
		}*/
		
		RequiredServiceInfo rsis = new RequiredServiceInfo(rs.name(), rs.type(), 
			rs.min(), rs.max(), binding, 
			//nfprops, 
			Arrays.asList(rs.tags())); // rs.multiple()
		
		return rsis;
	}

	/**
	 *  Create a service binding.
	 */
	public static RequiredServiceBinding createBinding(RequiredService rq)
	{
		UnparsedExpression	scopeexpression	= rq.scopeexpression()!=null && rq.scopeexpression().length()>0
				? new UnparsedExpression("scopeexpression", ServiceScope.class, rq.scopeexpression(), null) : null;

		return new RequiredServiceBinding(null, null, null,
			rq.scope(), MicroClassReader.createUnparsedExpressions(rq.interceptors()),
			rq.proxytype()
			)
			.setScopeExpression(scopeexpression);
	}
	
	/**
	 * 
	 */
	public static void checkAndAddRequiredServiceInfo(RequiredServiceInfo rsis, Map<String, RequiredServiceInfo> rsers, ClassLoader cl)
	{
		// Do not add definitions without name!
		if(rsis.getName()==null || rsis.getName().length()==0)
			return;
		
		if(rsers.containsKey(rsis.getName()))
		{
			RequiredServiceInfo old = (RequiredServiceInfo)rsers.get(rsis.getName());
			//if(old.isMultiple()!=rsis.isMultiple()
			if(old.getMin()!=rsis.getMin() || old.getMax()!=rsis.getMax() || !old.getType().getType(cl).equals(rsis.getType().getType(cl)))
				throw new RuntimeException("Extension hierarchy contains incompatible required service more than once: "+rsis.getName());
		}
		else
		{
			rsers.put(rsis.getName(), rsis);
		}
	}
}
