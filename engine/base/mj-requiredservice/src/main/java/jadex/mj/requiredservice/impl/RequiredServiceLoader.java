package jadex.mj.requiredservice.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jadex.common.UnparsedExpression;
import jadex.mj.feature.providedservice.ServiceScope;
import jadex.mj.micro.MicroClassReader;
import jadex.mj.requiredservice.RequiredServiceBinding;
import jadex.mj.requiredservice.RequiredServiceInfo;
import jadex.mj.requiredservice.annotation.RequiredService;
import jadex.mj.requiredservice.annotation.RequiredServices;

public class RequiredServiceLoader 
{
	public static Object readFeatureModel(final Class<?> clazz, ClassLoader cl)
	{
		Class<?> cma = clazz;
		
		Map<String, RequiredServiceInfo> rservices = new HashMap<>();
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
					
						checkAndAddRequiredServiceInfo(rsis, rservices, cl);
	
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
			}
			cma = cma.getSuperclass();
		}
		
		return rservices;//.values().toArray(new Object[rservices.size()]);
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
