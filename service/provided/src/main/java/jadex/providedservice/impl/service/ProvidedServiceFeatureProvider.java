package jadex.providedservice.impl.service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.injection.annotation.Provide;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.injection.impl.InjectionModel.FieldFetcher;
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
					});
				}
				
				
				// find fields with provided service anno.
				@SuppressWarnings("unchecked")
				Class<? extends Annotation>[] annos	= new Class[]{Provide.class, ProvideService.class};
				for(Class<? extends Annotation> anno: annos)
				{
					List<FieldFetcher>	fetchers	= InjectionModel.getFieldGetters(Collections.singletonList(pojoclazz), anno);
					if(fetchers!=null)
					{
						for(FieldFetcher fetcher: fetchers)
						{
							Set<Class<?>> fservices = findServiceInterfaces(fetcher.field().getType());
							// TODO: Service settings 
//							fetcher.field().getAnnotation(ProvideService.class);
							ret.add((comp, pojos, context) ->
							{
								Object value	= fetcher.fetcher().getValue(comp, pojos, context);
								if(value==null)
								{
									throw new RuntimeException("No value for provided service: "+fetcher.field());
								}
								ProvidedServiceFeature	feature	= (ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class);
								feature.addService(value, fetcher.field().getName(), fservices);
							});
						}
					}					
				}

				
				return ret;
			}
		});
	}
}
