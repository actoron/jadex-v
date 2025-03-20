package jadex.required2.impl;

import java.util.ArrayList;
import java.util.List;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.IValueFetcher;
import jadex.injection.impl.InjectionModel;
import jadex.provided2.annotation.Service;
import jadex.provided2.impl.search.ServiceQuery;
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
	
	//-------- augment injection feature with field injection --------
	
	static
	{
		InjectionModel.addValueFetcher(
			(pojotypes, valuetype) -> valuetype.isAnnotationPresent(Service.class) ? 
				((self, pojos, context) -> self.getFeature(IRequired2Feature.class).getLocalService(new ServiceQuery<>(valuetype))): null);
		
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
				List<IValueFetcher>	preparams	= new ArrayList<>();
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
					ISubscriptionIntermediateFuture<?> query	= self.getFeature(IRequired2Feature.class).addQuery(new ServiceQuery<>(fservice));
					query.next(result ->
					{
						invocation.handleInjection(self, pojos, result);
					});
				};
			}
			
			return ret;		
		});
	}
}
