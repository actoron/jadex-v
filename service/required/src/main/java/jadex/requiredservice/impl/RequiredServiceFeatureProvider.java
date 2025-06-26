package jadex.requiredservice.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
//		System.out.println("created required service feature: "+self);
		return new RequiredServiceFeature(self);
	}
	
	//-------- augment injection feature with field injection --------
	
	@Override
	public void init()
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
				ret	= (self, pojos, context, oldval) -> self.getFeature(IRequiredServiceFeature.class).getLocalService(new ServiceQuery<>((Class<?>)valuetype));
			}
			return ret;
		}, InjectService.class, Inject.class);
		
		// Multi service field / parameter (Subtypes of Collection)
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
						// Query into existing collection.
						if(anno instanceof InjectService && ((InjectService)anno).mode().equals(Mode.QUERY))
						{
							// Inject query results into existing collection.
							ret	= ((self, pojos, context, oldval) ->
							{
								@SuppressWarnings("unchecked")
								Collection<Object>	coll	= (Collection<Object>) oldval;
								if(coll==null)
								{
									throw new UnsupportedOperationException("Query injections only allowed for field with initial value: "+pojos.get(pojos.size()-1)+", "+valuetype+", "+anno);
								}
								
								self.getFeature(IRequiredServiceFeature.class).addQuery(new ServiceQuery<>((Class<?>)typeparam))
									.next(result -> coll.add(result));
								// Don't change field value, i.e. return original value.
								return oldval;
							});
						}
						
						// Set search result (ArrayList) as direct field value.
						else if(SReflect.isSupertype((Class<?>) generic.getRawType(), ArrayList.class))
						{
							ret	= ((self, pojos, context, oldval) ->
								self.getFeature(IRequiredServiceFeature.class).getLocalServices(new ServiceQuery<>((Class<?>)typeparam)));
						}
						
						// Copy search result into LinkedHashSet
						else if(SReflect.isSupertype((Class<?>) generic.getRawType(), LinkedHashSet.class))
						{
							ret	= ((self, pojos, context, oldval) ->
							{
								Set<Object>	set	= new LinkedHashSet<>();
								set.addAll(self.getFeature(IRequiredServiceFeature.class).getLocalServices(new ServiceQuery<>((Class<?>)typeparam)));
								return set;
							});
						}
						else
						{
							throw new UnsupportedOperationException("Only Collection, [Array]List and [[Linked]Hash]Set supported for collection injection: "+generic.getRawType());
						}
					}
				}
			}
			return ret;
			
		}, InjectService.class, Inject.class);
		
		// Single service method parameter.
		InjectionModel.addMethodInjection((classes, method, contextfetchers) ->
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
						preparams.add((self, pojos, context, oldval) -> context);
					}
				}
				IInjectionHandle	invocation	= InjectionModel.createMethodInvocation(method, classes, contextfetchers, preparams);
				
				Class<?>	fservice	= service;
				ret	= (self, pojos, context, oldval) ->
				{
					ISubscriptionIntermediateFuture<?> query	= self.getFeature(IRequiredServiceFeature.class).addQuery(new ServiceQuery<>(fservice));
					query.next(result ->
					{
						invocation.apply(self, pojos, result, null);
					});
					return null;
				};
			}
			
			return ret;		
		}, Inject.class, InjectService.class);
	}
}
