package jadex.providedservice.impl.service;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jadex.common.SUtil;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.injection.annotation.Inject;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.providedservice.IProvidedServiceFeature;
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
				
				
				// find fields with service type.
				Class<?> clazz	= pojoclazz;
				while(clazz!=null)
				{
					for(Field f: clazz.getDeclaredFields())
					{
						// Do not provide service if field is injected (i.e. required service).
						if(!f.isAnnotationPresent(Inject.class))
						{
							Set<Class<?>> fservices = findServiceInterfaces(f.getType());
							if(!fservices.isEmpty())
							{
								try
								{
									f.setAccessible(true);
									String	name	= f.getName();
									MethodHandle	fhandle	= MethodHandles.lookup().unreflectGetter(f);
									ret.add((comp, pojos, context) ->
									{
										try
										{
											ProvidedServiceFeature	feature	= (ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class);
											Object	servicepojo	= fhandle.invoke(pojos.get(pojos.size()-1));
											if(servicepojo==null)
											{
												throw new RuntimeException("No value for provided service: "+f);
											}
											feature.addService(servicepojo, name, fservices);
										}
										catch(Throwable e)
										{
											SUtil.throwUnchecked(e);
										}
									});
								}
								catch(IllegalAccessException e)
								{
									SUtil.throwUnchecked(e);
								}
							}
						}
					}
					
					clazz	= clazz.getSuperclass();
				}

				
				return ret;
			}
		});
	}
}
