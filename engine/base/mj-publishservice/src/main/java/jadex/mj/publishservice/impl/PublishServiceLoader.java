package jadex.mj.publishservice.impl;

import java.util.HashMap;
import java.util.Map;

import jadex.mj.feature.providedservice.impl.service.impl.ProvidedServiceInfo;
import jadex.mj.micro.MicroClassReader;
import jadex.mj.publishservice.publish.annotation.Publish;

public class PublishServiceLoader 
{
	public static Object readFeatureModel(final Class<?> clazz, ClassLoader cl)
	{
		Class<?> cma = clazz;
		
		Map<String, ProvidedServiceInfo> pservices = new HashMap<>();
		PublishServiceModel model = new PublishServiceModel();
		boolean pubsdone = false;
		
		while(cma!=null && !cma.equals(Object.class))
		{
			Map<String, PublishInfo> pubs = new HashMap<String, PublishInfo>();
			
			if(!pubsdone && MicroClassReader.isAnnotationPresent(cma, Publish.class, cl))
			{
				Publish[] ps = (Publish[])MicroClassReader.getAnnotations(cma, Publish.class, cl);
				for(Publish p: ps)
				{
					// todo: do we need this?! fixed way per annotation type?!
					//pubsdone = val.replace();
					
					PublishInfo pi = new PublishInfo(p.publishid(), p.publishtype(), p.publishtaget(), Object.class.equals(p.mapping())? null: p.mapping());
					model.addPublishInfo(pi);
				}
			}
			
			cma = cma.getSuperclass();
		}
				
		return model;
	}
}
