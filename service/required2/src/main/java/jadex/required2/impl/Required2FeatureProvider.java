package jadex.required2.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.required2.IRequired2Feature;

/**
 *  Provided services feature provider.
 */
public class Required2FeatureProvider extends ComponentFeatureProvider<IRequired2Feature>
{
	@Override
	public Class<IRequired2Feature> getFeatureType()
	{
		return IRequired2Feature.class;
	}

	@Override
	public IRequired2Feature createFeatureInstance(Component self)
	{
		return new Required2Feature(self);
	}
	
//	//-------- augment injection feature with new setup code --------
//	
//	static
//	{
//		InjectionModel.extra_onstart.add(new Function<Class<?>, List<IInjectionHandle>>()
//		{
//			@Override
//			public List<IInjectionHandle> apply(Class<?> pojoclazz)
//			{
//				List<IInjectionHandle>	ret	= new ArrayList<>();
//				
//				// find interfaces with service annotation on pojo
//				Set<Class<?>> services = findServiceInterfaces(pojoclazz);
//				if(!services.isEmpty())
//				{
//					ret.add((comp, pojo, context) ->
//					{
//						Required2Feature	feature	= (Required2Feature)comp.getFeature(IRequired2Feature.class);
//						feature.addService(pojo, null, services);
//					});
//				}
//				
//				
//				// find fields with service type.
//				Class<?> clazz	= pojoclazz;
//				while(clazz!=null)
//				{
//					for(Field f: clazz.getDeclaredFields())
//					{
//						Set<Class<?>> fservices = findServiceInterfaces(f.getType());
//						if(!fservices.isEmpty())
//						{
//							try
//							{
//								f.setAccessible(true);
//								String	name	= f.getName();
//								MethodHandle	fhandle	= MethodHandles.lookup().unreflectGetter(f);
//								ret.add((comp, pojo, context) ->
//								{
//									try
//									{
//										Required2Feature	feature	= (Required2Feature)comp.getFeature(IRequired2Feature.class);
//										feature.addService(fhandle.invoke(pojo), name, fservices);
//									}
//									catch(Throwable e)
//									{
//										SUtil.throwUnchecked(e);
//									}
//								});
//							}
//							catch(IllegalAccessException e)
//							{
//								SUtil.throwUnchecked(e);
//							}
//						}
//					}
//					
//					clazz	= clazz.getSuperclass();
//				}
//
//				
//				return ret;
//			}
//		});
//	}
}
