package jadex.providedservice.impl.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.injection.impl.InjectionModel.Getter;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.annotation.ProvideService;
import jadex.providedservice.annotation.Service;

/**
 *  Provided services feature provider.
 */
public class ProvidedServiceFeatureProvider extends ComponentFeatureProvider<IProvidedServiceFeature>
{
	protected static Set<Class< ? >> findServiceInterfaces(Class< ? > pojoclazz)
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
	
	static
	{
		InjectionModel.addExtraOnStart(new Function<Class<?>, List<IInjectionHandle>>()
		{
			@Override
			public List<IInjectionHandle> apply(Class<?> pojoclazz)
			{
				List<IInjectionHandle>	ret	= new ArrayList<>();
				
				// find interfaces with service annotation on pojo
				Set<Class<?>> services = findServiceInterfaces(pojoclazz);
				if(!services.isEmpty())
				{
					ret.add((comp, pojos, context) ->
					{
						ProvidedServiceFeature	feature	= (ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class);
						feature.addService(pojos.get(pojos.size()-1), null, services);
						return null;
					});
				}
				
				
				// find fields with provided service anno.
				List<Getter>	getters	= InjectionModel.getGetters(Collections.singletonList(pojoclazz), ProvideService.class);
				if(getters!=null)
				{
					for(Getter getter: getters)
					{
						Set<Class<?>> fservices = findServiceInterfaces(
							getter.member() instanceof Method
								? ((Method)getter.member()).getReturnType()
								: ((Field)getter.member()).getType());
						// TODO: Service settings 
//						if(getter.annotation() instanceof ProvideService)
						ret.add((comp, pojos, context) ->
						{
							Object value	= getter.fetcher().apply(comp, pojos, context);
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
			}
		});
	}
}
