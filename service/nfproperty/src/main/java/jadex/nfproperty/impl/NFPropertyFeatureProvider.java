package jadex.nfproperty.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.MethodInfo;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.injection.impl.InjectionModel.Getter;
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
	
	@Override
	public boolean isLazyFeature()
	{
		// initialization is triggered by injection, if necessary
		return true;
	}

	//-------- injection model extension --------
	
	@Override
	public void init()
	{
		InjectionModel.addPostInject((pojoclazzes, path, contextfetchers)->
		{
			List<IInjectionHandle>	ret	= new ArrayList<>();
			Class<?>	pojoclazz	= pojoclazzes.get(pojoclazzes.size()-1);
			
			// Add component properties
			Class<?>	test	= pojoclazz;
			while(test!=null)
			{
				if(test.isAnnotationPresent(NFProperties.class))
				{
					NFProperties	val	= test.getAnnotation(NFProperties.class);
					List<NFPropertyInfo> nfprops = NFPropertyLoader.createNFPropertyInfos(val);
					
					for(NFPropertyInfo nfprop: nfprops)
					{
						ret.add((comp, pojos, context, oldval) ->
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
			
			// Add properties for interfaces with service annotation on pojo
			addServicePropertyInjections(pojoclazz, ret);
			
			// Add properties for fields/methods with provided service anno.
			List<Getter>	getters	= InjectionModel.getGetters(Collections.singletonList(pojoclazz), ProvideService.class, contextfetchers);
			if(getters!=null)
			{
				for(Getter getter: getters)
				{
					Class<?>	clazz	= getter.member() instanceof Method
						? ((Method)getter.member()).getReturnType()
						: ((Field)getter.member()).getType();
					
					addServicePropertyInjections(clazz, ret);
				}
			}
			
			return ret;
		});
	}
	
	/**
	 *  Add properties injections for the given service pojo clazz. 
	 */
	protected static void addServicePropertyInjections(Class<?> pojoclazz, List<IInjectionHandle> ret)
	{
		Map<Class<?>, ProvideService> services = ProvidedServiceFeatureProvider.findServiceInterfaces(pojoclazz);
		
		for(Class<?> service: services.keySet())
		{
			Map<MethodInfo, List<NFPropertyInfo>> nfps = NFPropertyLoader.createProvidedNFProperties(pojoclazz, service);
			
			// Properties on service
			if(nfps.get(null)!=null)
			{
				ret.add((comp, pojos, context, oldval) ->
				{
					IService	ser	= (IService)comp.getFeature(IProvidedServiceFeature.class).getProvidedService(service);
					// TODO: wait for future
					((NFPropertyFeature)comp.getFeature(INFPropertyFeature.class)).addNFProperties(nfps.get(null), ser);
					return null;
				});
			}
			
			// Properties on methods
			nfps.entrySet().forEach(entry ->
			{
				if(entry!=null)
				{
					ret.add((comp, pojos, context, oldval) ->
					{
						IService	ser	= (IService)comp.getFeature(IProvidedServiceFeature.class).getProvidedService(service);
						// TODO: wait for future
						((NFPropertyFeature)comp.getFeature(INFPropertyFeature.class)).addNFMethodProperties(entry.getValue(), ser, entry.getKey());
						return null;
					});
				}
			});
		}
	}
}

