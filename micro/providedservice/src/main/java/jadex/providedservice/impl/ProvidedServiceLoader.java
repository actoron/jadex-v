package jadex.providedservice.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.MethodInfo;
import jadex.common.SReflect;
import jadex.common.UnparsedExpression;
import jadex.core.impl.ComponentManager;
import jadex.future.IFuture;
import jadex.micro.MicroClassReader;
import jadex.model.annotation.Value;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.providedservice.annotation.Security;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.Tag;
import jadex.providedservice.annotation.Tags;
import jadex.providedservice.impl.service.BasicService;
import jadex.providedservice.impl.service.ProvidedServiceImplementation;
import jadex.providedservice.impl.service.ProvidedServiceInfo;
import jadex.providedservice.impl.service.ProvidedServiceModel;

public class ProvidedServiceLoader 
{
	public static Object readFeatureModel(final Class<?> clazz, ClassLoader cl)
	{
		Class<?> cma = clazz;
		
		ProvidedServiceModel model = new ProvidedServiceModel();
		boolean prosdone = false;
		
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
					if(!model.hasService(vals[i].name()))
					{
						ProvidedServiceInfo psi = createProvidedServiceInfo(vals[i], clazz);
						//String name = vals[i].name().length()==0? ("#"+cnt++): vals[i].name();
						model.addService(psi);
						//pservices.put(vals[i].name().length()==0? ("#"+cnt++): vals[i].name(), psi);
					}
				}
			}
			
			// check interfaces and add those which have service annotation to provided service interfaces
			Class<?>[] ifaces = cma.getInterfaces();
			for(Class<?> iface: ifaces)
			{
				if(MicroClassReader.isAnnotationPresent(iface, Service.class, cl))
				{
					serifaces.add(iface);
				}
			}
			
			cma = cma.getSuperclass();
		}
		
		// Check if there are implemented service interfaces for which the agent
		// does not have a provided service declaration (implementation=agent)
		if(!serifaces.isEmpty())
		{
			//ProvidedServiceInfo[] psis = (ProvidedServiceInfo[])pservices.values().toArray(new ProvidedServiceInfo[pservices.size()]);
			ProvidedServiceInfo[] psis = model.getServices();
			for(ProvidedServiceInfo psi: psis)
			{
				String val = psi.getImplementation().getValue();
				if(psi.getImplementation().getClazz()!=null || (val!=null && val.length()!=0))
					//&& (val.equals("$pojoagent") || val.equals("$pojoagent!=null? $pojoagent: $component"))))
				{
					Class<?> tt = psi.getType().getType(cl);
					serifaces.remove(tt);
				}
			}
			
			// All interfaces that are still in the set do not have an implementation
			for(Class<?> iface: serifaces)
			{
				//ProvidedServiceImplementation impl = new ProvidedServiceImplementation(null, "$pojoagent!=null? $pojoagent: $component", Implementation.PROXYTYPE_DECOUPLED, null);
				ProvidedServiceImplementation impl = new ProvidedServiceImplementation(null, null, Implementation.PROXYTYPE_DECOUPLED, null);
				ProvidedServiceInfo psi = new ProvidedServiceInfo(BasicService.generateServiceName(iface), iface, impl);
				Collection<String> tags = createTags(clazz, iface);
				if(tags!=null && tags.size()>0)
					psi.setTags(tags);
				
				//String name = psi.getName()==null? ("#"+cnt++): psi.getName();
				model.addService(psi);
				//pservices.put(psi.getName()==null? ("#"+cnt++): psi.getName(), psi);
			}
		}
				
		return model;
	}
	
	/**
	 *  Create info from annotation.
	 */
	protected static ProvidedServiceInfo createProvidedServiceInfo(ProvidedService prov, Class<?> clazz)
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
		
		//Publish p = prov.publish();
		
		//PublishInfo pi = p.publishid().length()==0? null: new PublishInfo(p.publishid(), p.publishtype(), p.publishscope(), p.multi(),
		//	Object.class.equals(p.mapping())? null: p.mapping(), MicroClassReader.createUnparsedExpressions(p.properties()));
		
		UnparsedExpression	scopeexpression	= prov.scopeexpression()!=null && prov.scopeexpression().length()>0
				? new UnparsedExpression("scopeexpression", ServiceScope.class, prov.scopeexpression(), null) : null;

		// Only keep security settings explicitly set in @ProvidedService annotation (default: empty roles)
		Security security = prov.security().roles().length>0 ? prov.security() : null;
		
		//NameValue[] props = prov.properties();
		//List<UnparsedExpression> serprops = (props != null && props.length > 0) ? new ArrayList<UnparsedExpression>(Arrays.asList(MicroClassReader.createUnparsedExpressions(props))) : null;
		
		Class<?> implcl = null;
		if(impl.getClazz()!=null)
			implcl = impl.getClazz().getType(ComponentManager.get().getClassLoader());
		else if(impl.getClazz()==null && impl.getValue()==null)
			implcl = clazz; // pojoclass
		
		Collection<String> tags = createTags(implcl, prov.type());
		
		String name = prov.name().length()>0? prov.name(): BasicService.generateServiceName(prov.type()); // null
		ProvidedServiceInfo psi = new ProvidedServiceInfo(name, prov.type(), impl,  prov.scope(), scopeexpression, security, tags); 
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
	
	/**
	 *  Create tag strings (unparsed) from Tags and Tag annotations.
	 */
	public static Collection<String> createTags(Class<?> impltype, Class<?> sertype)
	{
		List<String> ret = new ArrayList<String>();
		
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
		
		for(Class<?> sclazz: classes)
		{
			if(sclazz.isAnnotationPresent(Tags.class))
			{
				Tags tags = sclazz.getAnnotation(Tags.class);
				for(Tag tag: tags.value())
				{
					ret.add(tag.value());
				}
			}
			
			if(sclazz.isAnnotationPresent(Tag.class))
			{
				Tag[] tags = sclazz.getAnnotationsByType(Tag.class);
				for(Tag tag: tags)
				{
					ret.add(tag.value());
				}
			}
		}
		
		return ret;
	}
	
}
