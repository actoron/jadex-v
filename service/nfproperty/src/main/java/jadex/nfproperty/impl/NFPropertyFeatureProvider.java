package jadex.nfproperty.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.nfproperty.INFProperty;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.impl.modelinfo.NFPropertyInfo;
import jadex.requiredservice.IRequiredServiceFeature;


public class NFPropertyFeatureProvider extends ComponentFeatureProvider<INFPropertyFeature>
{	
	@Override
	public Class<INFPropertyFeature> getFeatureType()
	{
		return INFPropertyFeature.class;
	}

	@Override
	public INFPropertyFeature createFeatureInstance(Component self)
	{
		return new NFPropertyFeature(self);
	}
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		return Set.of(IRequiredServiceFeature.class);
	}

	//-------- injection model extension --------
	
	static
	{
		InjectionModel.addExtraOnStart(new Function<Class<?>, List<IInjectionHandle>>()
		{
			@Override
			public List<IInjectionHandle> apply(Class<?> pojoclazz)
			{
				List<IInjectionHandle>	ret	= new ArrayList<>();
				
				// Find class with property annotations.
				Class<?>	test	= pojoclazz;
				while(test!=null)
				{
					if(test.isAnnotationPresent(NFProperties.class))
					{
						NFProperties	val	= test.getAnnotation(NFProperties.class);
						List<NFPropertyInfo> nfprops = NFPropertyLoader.createNFPropertyInfos(val);
						
						for(NFPropertyInfo nfprop: nfprops)
						{
							ret.add((comp, pojos, context) ->
							{
								
								Class<?> clazz = nfprop.getClazz().getType(pojos.get(pojos.size()-1).getClass().getClassLoader());
								INFProperty<?, ?> nfp = AbstractNFProperty.createProperty(clazz, comp, null, null, nfprop.getParameters());
								// TODO: wait for future
								comp.getFeature(INFPropertyFeature.class).getComponentPropertyProvider().addNFProperty(nfp);
								return null;
							});
						}
					}
					
					test	= test.getSuperclass();
				}
//				
//				// Find fields with publish annotation.
//				for(Field f: InjectionModel.findFields(pojoclazz, Publish.class))
//				{
//					try
//					{
//						PublishInfo pi = getPublishInfo(f.getAnnotation(Publish.class));
//	
//						f.setAccessible(true);
//						MethodHandle	fhandle	= MethodHandles.lookup().unreflectGetter(f);
//						ret.add((comp, pojos, context) ->
//						{
//							try
//							{
//								IPublishServiceFeature	feature	= comp.getFeature(IPublishServiceFeature.class);
//								Object	servicepojo	= fhandle.invoke(pojos.get(pojos.size()-1));
//								if(servicepojo==null)
//								{
//									throw new RuntimeException("No value for provided service: "+f);
//								}
//								// do we want to chain the publication on serviceStart and serviceEnd of each service?!
//								// how could this be done? with listeners on other feature?!
//								feature.publishService((IService)servicepojo, pi).get();
//								return null;
//							}
//							catch(Throwable e)
//							{
//								throw SUtil.throwUnchecked(e);
//							}
//						});
//					}
//					catch(Exception e)
//					{
//						SUtil.throwUnchecked(e);
//					}
//				}
				return ret;
			}
		});
	}
}

