package jadex.publishservice.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.injection.impl.InjectionModel;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.publish.annotation.Publish;

public abstract class PublishServiceFeatureProvider	extends ComponentFeatureProvider<IPublishServiceFeature>
{
	@Override
	public boolean isLazyFeature()
	{
		// is lazy and triggered by injection
		return true;
	}
	
	//-------- injection model extension --------
	
	@Override
	public void init()
	{
		InjectionModel.addExtraCode(model ->
		{
			// Find class with publish annotation.
			Class<?>	test	= model.getPojoClazz();
			while(test!=null)
			{
				if(test.isAnnotationPresent(Publish.class))
				{
					Publish	publish	= test.getAnnotation(Publish.class);
					PublishInfo pi = getPublishInfo(publish);
					model.addPostInject((comp, pojos, context, oldval) ->
					{
						IProvidedServiceFeature	prov	= comp.getFeature(IProvidedServiceFeature.class);
						Object	service	= prov.getProvidedService(publish.publishtarget());
						
						IPublishServiceFeature	feature	= comp.getFeature(IPublishServiceFeature.class);
						// do we want to chain the publication on serviceStart and serviceEnd of each service?!
						// how could this be done? with listeners on other feature?!
						feature.publishService((IService)service, pi).get();
						return null;
					});
				}
				
				test	= test.getSuperclass();
			}
			
			// Find fields with publish annotation.
			for(Field f: model.findFields(Publish.class))
			{
				try
				{
					PublishInfo pi = getPublishInfo(f.getAnnotation(Publish.class));

					f.setAccessible(true);
					MethodHandle	fhandle	= MethodHandles.lookup().unreflectGetter(f);
					model.addPostInject((comp, pojos, context, oldval) ->
					{
						try
						{
							IPublishServiceFeature	feature	= comp.getFeature(IPublishServiceFeature.class);
							Object	servicepojo	= fhandle.invoke(pojos.get(pojos.size()-1));
							if(servicepojo==null)
							{
								throw new RuntimeException("No value for provided service: "+f);
							}
							// do we want to chain the publication on serviceStart and serviceEnd of each service?!
							// how could this be done? with listeners on other feature?!
							feature.publishService((IService)servicepojo, pi).get();
							return null;
						}
						catch(Throwable e)
						{
							throw SUtil.throwUnchecked(e);
						}
					});
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
		});
	}
		
	/**
	 *  Convert annotation to info object.
	 */
	// TODO: Just use annotation?
	protected static PublishInfo getPublishInfo(Publish p)
	{
		String pt = p.publishtagetname().length()>0? p.publishtagetname(): null;
		if(pt==null && !p.publishtarget().equals(Object.class))
			pt = SReflect.getClassName(p.publishtarget());
		
		PublishInfo pi = new PublishInfo(p.publishid(), p.publishtype(), pt, Object.class.equals(p.mapping())? null: p.mapping());
		return pi;
	}
}
