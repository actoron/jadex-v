package jadex.provided2.impl;

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
import jadex.provided2.IProvided2Feature;
import jadex.provided2.annotation.Service;

/**
 *  Provided services feature provider.
 */
public class Provided2FeatureProvider extends ComponentFeatureProvider<IProvided2Feature>
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
	public Class<IProvided2Feature> getFeatureType()
	{
		return IProvided2Feature.class;
	}

	@Override
	public IProvided2Feature createFeatureInstance(Component self)
	{
		return new Provided2Feature(self);
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
						Provided2Feature	feature	= (Provided2Feature)comp.getFeature(IProvided2Feature.class);
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
											Provided2Feature	feature	= (Provided2Feature)comp.getFeature(IProvided2Feature.class);
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
