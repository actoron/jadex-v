package jadex.mj.feature.providedservice.impl.service.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.Boolean3;
import jadex.common.SReflect;
import jadex.common.UnparsedExpression;
import jadex.future.IFuture;
import jadex.mj.core.annotation.NameValue;
import jadex.mj.core.annotation.Value;
import jadex.mj.core.modelinfo.ModelInfo;
import jadex.mj.feature.providedservice.IMjProvidedServiceFeature;
import jadex.mj.feature.providedservice.ServiceScope;
import jadex.mj.feature.providedservice.annotation.Implementation;
import jadex.mj.feature.providedservice.annotation.ProvidedService;
import jadex.mj.feature.providedservice.annotation.ProvidedServices;
import jadex.mj.feature.providedservice.annotation.Publish;
import jadex.mj.feature.providedservice.annotation.Security;
import jadex.mj.micro.MicroClassReader;

public class ProvidedServiceLoader 
{
	public static Object readFeatureModel(final Class<?> clazz, ClassLoader cl)
	{
		Class<?> cma = clazz;
		
		int cnt = 0;
		Map<String, ProvidedServiceInfo> pservices = new HashMap<>();
		boolean prosdone = false;
		
		Boolean3 autoprovide = Boolean3.NULL;
		Set<Class<?>> serifaces = new HashSet<Class<?>>(); 
		
		while(cma!=null && !cma.equals(Object.class))
		{
			// Take all but new overrides old
			if(!prosdone && MicroClassReader.isAnnotationPresent(cma, ProvidedServices.class, cl))
			{
				ProvidedServices val = (ProvidedServices)MicroClassReader.getAnnotation(cma, ProvidedServices.class, cl);
				ProvidedService[] vals = val.value();
				prosdone = val.replace();
				
				for(int i=0; i<vals.length; i++)
				{
					if(vals[i].name().length()==0 || !pservices.containsKey(vals[i].name()))
					{
						ProvidedServiceInfo psi = createProvidedServiceInfo(vals[i]);
						pservices.put(vals[i].name().length()==0? ("#"+cnt++): vals[i].name(), psi);
					}
				}
			}
			
			cma = cma.getSuperclass();
		}
		
		// Check if there are implemented service interfaces for which the agent
		// does not have a provided service declaration (implementation=agent)
		if(autoprovide.isTrue() && !serifaces.isEmpty())
		{
			ProvidedServiceInfo[] psis = (ProvidedServiceInfo[])pservices.values().toArray(new ProvidedServiceInfo[pservices.size()]);
			for(ProvidedServiceInfo psi: psis)
			{
				String val = psi.getImplementation().getValue();
				if(psi.getImplementation().getClazz()!=null || (val!=null && val.length()!=0 
					&& (val.equals("$pojoagent") || val.equals("$pojoagent!=null? $pojoagent: $component"))))
				{
					Class<?> tt = psi.getType().getType(cl);
					serifaces.remove(tt);
				}
			}
			
			// All interfaces that are still in the set do not have an implementation
			for(Class<?> iface: serifaces)
			{
				ProvidedServiceImplementation impl = new ProvidedServiceImplementation(null, "$pojoagent!=null? $pojoagent: $component", Implementation.PROXYTYPE_DECOUPLED, null);
				ProvidedServiceInfo psi = new ProvidedServiceInfo(null, iface, impl);
				pservices.put(psi.getName()==null? ("#"+cnt++): psi.getName(), psi);
			}
		}
				
		return pservices.values().toArray(new ProvidedServiceInfo[pservices.size()]);
	}
	
	/**
	 *  Create info from annotation.
	 */
	protected static ProvidedServiceInfo createProvidedServiceInfo(ProvidedService prov)
	{
		Implementation im = prov.implementation();
		Value[] inters = im.interceptors();
		UnparsedExpression[] interceptors = null;
		if(inters.length>0)
		{
			interceptors = new UnparsedExpression[inters.length];
			for(int k=0; k<inters.length; k++)
			{
				interceptors[k] = new UnparsedExpression(null, inters[k].clazz(), inters[k].value(), null);
			}
		}
		//RequiredServiceBinding bind = null;//createBinding(im.binding());
		ProvidedServiceImplementation impl = new ProvidedServiceImplementation(!im.value().equals(Object.class)? im.value(): null, 
			im.expression().length()>0? im.expression(): null, im.proxytype(), 
			//bind, 
			interceptors);
		
		Publish p = prov.publish();
		
		PublishInfo pi = p.publishid().length()==0? null: new PublishInfo(p.publishid(), p.publishtype(), p.publishscope(), p.multi(),
			Object.class.equals(p.mapping())? null: p.mapping(), MicroClassReader.createUnparsedExpressions(p.properties()));
		
		UnparsedExpression	scopeexpression	= prov.scopeexpression()!=null && prov.scopeexpression().length()>0
				? new UnparsedExpression("scopeexpression", ServiceScope.class, prov.scopeexpression(), null) : null;

		// Only keep security settings explicitly set in @ProvidedService annotation (default: empty roles)
		Security security = prov.security().roles().length>0 ? prov.security() : null;
		
		NameValue[] props = prov.properties();
		List<UnparsedExpression> serprops = (props != null && props.length > 0) ? new ArrayList<UnparsedExpression>(Arrays.asList(MicroClassReader.createUnparsedExpressions(props))) : null;
		
		ProvidedServiceInfo psi = new ProvidedServiceInfo(prov.name().length()>0? prov.name(): null, prov.type(), impl,  prov.scope(), scopeexpression, security, 
			null, //pi, 
			serprops);
		return psi;
	}
	
	/**
	 *  Check, if the return type of the agent method is acceptable.
	 */
	protected void checkMethodReturnType(Class<? extends Annotation> ann, Method m, ClassLoader cl)
	{
		// Todo: allow other return types than void 
		boolean	isvoid	= m.getReturnType().equals(void.class);
		boolean isfuture = !isvoid && SReflect.isSupertype(getClass(IFuture.class, cl), m.getReturnType());
		if(isfuture)
		{
			Type	t	= m.getGenericReturnType();
			isvoid	= !(t instanceof ParameterizedType);	// Assume void when no future type given.
			if(!isvoid)
			{
				ParameterizedType	p	= (ParameterizedType)t;
				Type[]	ts	= p.getActualTypeArguments();
				isvoid	= ts.length==1 && ts[0].equals(Void.class);
			}
		}
		
		if(!isvoid)
			throw new RuntimeException("@"+ann.getSimpleName()+" method requires return type 'void' or 'IFuture<Void>': "+m);
	}
	
	/**
	 * 
	 */
	public static Class<?> getClass(Class<?> clazz, ClassLoader cl)
	{
		Class<?> ret = clazz;
		
		try
		{
			if(!clazz.getClassLoader().equals(cl))
			{
				ret = cl.loadClass(clazz.getName());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return ret;
	}
	/*
	public static boolean isAnnotationPresent(Class<?> clazz, Class<? extends Annotation> anclazz, ClassLoader cl)
	{
		return clazz.isAnnotationPresent((Class<? extends Annotation>)getClass(anclazz, cl));
	}
	
	public static boolean isAnnotationPresent(Field f, Class<? extends Annotation> anclazz, ClassLoader cl)
	{
		return f.isAnnotationPresent((Class<? extends Annotation>)getClass(anclazz, cl));
	}
	
	public static boolean isAnnotationPresent(Method m, Class<? extends Annotation> anclazz, ClassLoader cl)
	{
		return m.isAnnotationPresent((Class<? extends Annotation>)getClass(anclazz, cl));
	}
	
	public static boolean isAnnotationPresent(Constructor<?> con, Class<? extends Annotation> anclazz, ClassLoader cl)
	{
		return con.isAnnotationPresent((Class<? extends Annotation>)getClass(anclazz, cl));
	}
	
	public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> anclazz, ClassLoader cl)
	{
		ClassLoader cl2 = cl instanceof DummyClassLoader? ((DummyClassLoader)cl).getOriginal(): cl;
		return getProxyAnnotation(clazz.getAnnotation((Class<T>)getClass(anclazz, cl)), cl2);
	}
	
	public static <T extends Annotation> T getAnnotation(Field f, Class<T> anclazz, ClassLoader cl)
	{
		ClassLoader cl2 = cl instanceof DummyClassLoader? ((DummyClassLoader)cl).getOriginal(): cl;
		return getProxyAnnotation(f.getAnnotation((Class<T>)getClass(anclazz, cl)), cl2);
	}
	
	public static <T extends Annotation> T getAnnotation(Method m, Class<T> anclazz, ClassLoader cl)
	{
		ClassLoader cl2 = cl instanceof DummyClassLoader? ((DummyClassLoader)cl).getOriginal(): cl;
		return getProxyAnnotation(m.getAnnotation((Class<T>)getClass(anclazz, cl)), cl2);
	}
	
	public static <T extends Annotation> T getAnnotation(Constructor<?> c, Class<T> anclazz, ClassLoader cl)
	{
		ClassLoader cl2 = cl instanceof DummyClassLoader? ((DummyClassLoader)cl).getOriginal(): cl;
		return getProxyAnnotation(c.getAnnotation((Class<T>)getClass(anclazz, cl)), cl2);
	}*/
}
