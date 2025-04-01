package jadex.nfproperty.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jadex.common.MethodInfo;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.nfproperty.INFProperty;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.impl.modelinfo.NFPropertyInfo;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.annotation.ProvideService;
import jadex.providedservice.impl.service.ProvidedServiceFeatureProvider;
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
		return Set.of(IProvidedServiceFeature.class, IRequiredServiceFeature.class);
	}

	//-------- injection model extension --------
	
	@Override
	public void init()
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
				
				// find interfaces with service annotation on pojo
				Map<Class<?>, ProvideService> services = ProvidedServiceFeatureProvider.findServiceInterfaces(pojoclazz);
				if(!services.isEmpty())
				{
					for(Class<?> service: services.keySet())
					{
						Map<MethodInfo, List<NFPropertyInfo>> nfps = NFPropertyLoader.createProvidedNFProperties(pojoclazz, getClass());
						nfps.entrySet().forEach(entry ->
						{
							ret.add((comp, pojos, context) ->
							{
								IService	ser	= (IService)comp.getFeature(IProvidedServiceFeature.class).getProvidedService(service);
								// TODO: wait for future
								((NFPropertyFeature)comp.getFeature(INFPropertyFeature.class)).addNFMethodProperties(entry.getValue(), ser, entry.getKey());
								return null;
							});
						});
						
//						List<NFPropertyInfo> snfps = fmymodel.getProvidedServiceProperties(name);
//						if(snfps!=null)
//						{
//							bar.add(addNFProperties(snfps, ser));
//						}
					}
				}
				
				
//				// find fields/methods with provided service anno.
//				List<Getter>	getters	= InjectionModel.getGetters(Collections.singletonList(pojoclazz), ProvideService.class);
//				if(getters!=null)
//				{
//					for(Getter getter: getters)
//					{
//						Map<Class<?>, ProvideService> fservices = findServiceInterfaces(
//							getter.member() instanceof Method
//								? ((Method)getter.member()).getReturnType()
//								: ((Field)getter.member()).getType());
//						
//						if(fservices.isEmpty())
//						{
//							throw new RuntimeException("No service interfaces found on: "+getter.member());
//						}
//						
//						// TODO: Service settings 
////						if(getter.annotation() instanceof ProvideService)
//						ret.add((comp, pojos, context) ->
//						{
//							Object value	= getter.fetcher().apply(comp, pojos, context);
//							if(value==null)
//							{
//								throw new RuntimeException("No value for provided service: "+getter.member());
//							}
//							ProvidedServiceFeature	feature	= (ProvidedServiceFeature)comp.getFeature(IProvidedServiceFeature.class);
//							feature.addService(value, getter.member().getName(), fservices);
//							return null;
//						});
//					}
//				}
				
				return ret;
			}
		});
	}
}

