package jadex.providedservice.impl.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.injection.annotation.Inject;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.injection.impl.InjectionModel.Getter;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.ProvideService;
import jadex.providedservice.annotation.Service;

/**
 *  Provided services feature provider.
 */
public class ProvidedServiceFeatureProvider extends ComponentFeatureProvider<IProvidedServiceFeature>
{
	/**
	 *  Get the direct service types of a pojo class
	 *  or field/method (return) type.
	 *  
	 *  In case of pojo class, also return ProvideService annotations if any.
	 */
	public static Map<Class<?>, ProvideService> findServiceInterfaces(Class< ? > pojoclazz)
	{
		Map<Class<?>, ProvideService>	services	= new LinkedHashMap<>();
		
		// When provided service is from field or method
		// -> static type might be interface already.
		if(pojoclazz.isInterface() && pojoclazz.isAnnotationPresent(Service.class))
		{
			services.put(pojoclazz, null);
		}
		
		// When type is impl type (e.g. provided service on class)
		// -> search for directly implemented service interfaces in class hierarchy.
		else
		{
			Class<?>	clazz	= pojoclazz;
			while(clazz!=null)
			{
				// TODO: multiple provided service annos, when multiple service interfaces
				ProvideService	anno	= null;
				if(clazz.isAnnotationPresent(ProvideService.class))
				{
					anno	= clazz.getAnnotation(ProvideService.class);
				}
				
				for(Class<?> interfaze: clazz.getInterfaces())
				{
					if(interfaze.isAnnotationPresent(Service.class))
					{
//						// TODO: specify type on anno, when multiple service interfaces
//						if(!anno.getType().equals(interfaze))
//						{
//							services.put(interfaze, null);
//						}
//						else
						services.put(interfaze, anno);
					}
				}
				
				if(anno!=null && !services.values().contains(anno))
				{
					throw new RuntimeException("No service interface for @ProvideService: "+clazz+", "+anno);
				}
				clazz	= clazz.getSuperclass();
			}
		}
		
		return services;
	}
	
	/**
	 *  Find super interfaces for a service interface,
	 *  i.e., when an implemented interface of a service interface
	 *  is also annotated with Service anno.
	 */
	protected static Set<Class< ? >> findSuperInterfaces(Class< ? > pojoclazz)
	{
		Set<Class<?>>	services	= new LinkedHashSet<>();
		
		if(pojoclazz.isInterface() && pojoclazz.isAnnotationPresent(Service.class))
		{
			services.add(pojoclazz);
		}
		
		Class<?>	clazz	= pojoclazz;
		while(clazz!=null)
		{
			List<Class<?>> interfaces	= new ArrayList<Class<?>>();
			interfaces.addAll(Arrays.asList(clazz.getInterfaces()));
			while(!interfaces.isEmpty())
			{
				Class<?>	interfaze	= interfaces.removeLast();
				
				if(interfaze.isAnnotationPresent(Service.class))
				{
					services.add(interfaze);
				}
				
				interfaces.addAll(Arrays.asList(interfaze.getInterfaces()));	
			}
			
			clazz	= clazz.getSuperclass();
		}
		return services;
	}

	@Override
	public Class<IProvidedServiceFeature> getFeatureType()
	{
		return IProvidedServiceFeature.class;
	}

	@Override
	public IProvidedServiceFeature createFeatureInstance(Component self)
	{
		return new ProvidedServiceFeature(self);
	}
	
	//-------- augment injection feature with new setup code --------
	
	@Override
	public void init()
	{
		// Add IServiceIdentifier injection
		InjectionModel.addValueFetcher((pojotypes, valuetype, annotation) ->
		{
			IInjectionHandle	ret	= null;
			if(IServiceIdentifier.class.equals(valuetype))
			{
				// find interfaces with service annotation on pojo
				Map<Class<?>, ProvideService> services = findServiceInterfaces(pojotypes.get(pojotypes.size()-1));
				if(services.size()==1)
				{
					ret	= (comp, pojos, context, oldval) ->
					{
						ProvidedServiceFeature	feature	= (ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class);
						Object	service	= feature.getProvidedService(services.keySet().iterator().next());
						return ((IService)service).getServiceId();
					};
				}
				else if(!services.isEmpty())
				{
					throw new RuntimeException("Cannot inject IServiceIdentifier for multi-type service: "+pojotypes.get(pojotypes.size()-1));
				}
				else
				{
					throw new RuntimeException("Cannot inject IServiceIdentifier in non-service pojo: "+pojotypes.get(pojotypes.size()-1));
				}
			}
			return ret;
		}, Inject.class);
		
		
		// Provide services from class (do pre-inject so SID injection works, hack?)
		InjectionModel.addPreInject((pojoclazzes, contextfetchers) ->
		{
			List<IInjectionHandle>	ret	= new ArrayList<>();
			
			// find interfaces with service annotation on pojo
			Map<Class<?>, ProvideService> services = findServiceInterfaces(pojoclazzes.get(pojoclazzes.size()-1));
			if(!services.isEmpty())
			{
				// TODO: Service settings 
//					if(getter.annotation() instanceof ProvideService)
				ret.add((comp, pojos, context, oldval) ->
				{
					ProvidedServiceFeature	feature	= (ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class);
					feature.addService(pojos.get(pojos.size()-1), null, services);
					return null;
				});
			}
			
			return ret;
		});
			
		// Provide services from field and method annotations
		InjectionModel.addPostInject((pojoclazzes, contextfetchers) ->
		{
			List<IInjectionHandle>	ret	= new ArrayList<>();
			
			// find fields/methods with provided service anno.
			List<Getter>	getters	= InjectionModel.getGetters(pojoclazzes, ProvideService.class, contextfetchers);
			if(getters!=null)
			{
				for(Getter getter: getters)
				{
					Map<Class<?>, ProvideService> fservices = findServiceInterfaces(
						getter.member() instanceof Method
							? ((Method)getter.member()).getReturnType()
							: ((Field)getter.member()).getType());
					
					if(fservices.isEmpty())
					{
						throw new RuntimeException("No service interfaces found on: "+getter.member());
					}
					
					// TODO: Service settings 
//						if(getter.annotation() instanceof ProvideService)
					ret.add((comp, pojos, context, oldval) ->
					{
						Object value	= getter.fetcher().apply(comp, pojos, context, null);
						if(value==null)
						{
							throw new RuntimeException("No value for provided service: "+getter.member());
						}
						ProvidedServiceFeature	feature	= (ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class);
						feature.addService(value, getter.member().getName(), fservices);
						return null;
					});
				}
			}

			return ret;
		});
	}
}
