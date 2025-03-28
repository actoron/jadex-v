package jadex.requiredservice.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jadex.common.SReflect;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.annotation.InjectService;
import jadex.requiredservice.annotation.InjectService.Mode;

/**
 *  Provided services feature provider.
 */
public class RequiredServiceFeatureProvider extends ComponentFeatureProvider<IRequiredServiceFeature>
{
	@Override
	public Class<IRequiredServiceFeature> getFeatureType()
	{
		return IRequiredServiceFeature.class;
	}

	@Override
	public IRequiredServiceFeature createFeatureInstance(Component self)
	{
		return new RequiredServiceFeature(self);
	}
	
	//-------- augment injection feature with field injection --------
	
	static
	{
		// Single service field / parameter.
		InjectionModel.addValueFetcher(
			(pojotypes, valuetype, anno) -> 
		{
			IInjectionHandle	ret	= null;
			if(valuetype instanceof Class && ((Class<?>)valuetype).isAnnotationPresent(Service.class))
			{
				if(anno instanceof InjectService && ((InjectService)anno).mode()==Mode.QUERY)
				{
					throw new UnsupportedOperationException("Mode query not supportet for skalar fields/parameter: "+pojotypes.get(pojotypes.size()-1)+", "+valuetype+", "+anno);
				}					
				ret	= (self, pojos, context) -> self.getFeature(IRequiredServiceFeature.class).getLocalService(new ServiceQuery<>((Class<?>)valuetype));
			}
			return ret;
		}, InjectService.class, Inject.class);
		
		// Multi service field / parameter (Set etc.)
		InjectionModel.addValueFetcher(
			(pojotypes, valuetype, anno) ->
		{
			IInjectionHandle	ret	= null;
			
			if(((Type)valuetype) instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)((Type)valuetype);
				if((generic.getRawType() instanceof Class<?>) && SReflect.isSupertype(Collection.class, (Class<?>)generic.getRawType())
					&& generic.getActualTypeArguments().length==1)
				{
					Type	typeparam	= generic.getActualTypeArguments()[0];
					if(typeparam instanceof Class<?> && ((Class<?>)typeparam).isAnnotationPresent(Service.class))
					{
						System.out.println("services injection: "+generic.getRawType()+" of "+typeparam);
//						if(anno instanceof InjectService && )
//						if()
						
						ret	= ((self, pojos, context) ->
							self.getFeature(IRequiredServiceFeature.class).getLocalServices(new ServiceQuery<>((Class<?>)typeparam)));
					}
				}
			}
			return ret;
			
		}, InjectService.class, Inject.class);
		
		// Single service method parameter.
		InjectionModel.addMethodInjection((classes, method) ->
		{
			IInjectionHandle	ret	= null;
			
			Class<?>[]	ptypes	= method.getParameterTypes();
			Class<?>	service	= null;
			int	index	= -1;
			for(int i=0; i<ptypes.length; i++)
			{
				if(ptypes[i].isAnnotationPresent(Service.class))
				{
					if(service!=null)
					{
						throw new UnsupportedOperationException("Only one service can be injected per method: "+method);
					}
					service	= ptypes[i];
					index	= i;
				}
			}
			
			if(service!=null)
			{
				List<IInjectionHandle>	preparams	= new ArrayList<>();
				for(int i=0; i<=index; i++)
				{
					if(i<index)
					{
						preparams.add(null);
					}
					else
					{
						preparams.add((self, pojos, context) -> context);
					}
				}
				IInjectionHandle	invocation	= InjectionModel.createMethodInvocation(method, classes, preparams);
				
				Class<?>	fservice	= service;
				ret	= (self, pojos, context) ->
				{
					ISubscriptionIntermediateFuture<?> query	= self.getFeature(IRequiredServiceFeature.class).addQuery(new ServiceQuery<>(fservice));
					query.next(result ->
					{
						invocation.apply(self, pojos, result);
					});
					return null;
				};
			}
			
			return ret;		
		}, Inject.class, InjectService.class);
	}
}
