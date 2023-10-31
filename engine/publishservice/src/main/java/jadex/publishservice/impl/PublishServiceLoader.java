package jadex.publishservice.impl;

import java.util.HashMap;
import java.util.Map;

import jadex.common.SReflect;
import jadex.micro.MicroClassReader;
import jadex.providedservice.impl.service.impl.ProvidedServiceInfo;
import jadex.publishservice.publish.annotation.Publish;

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
					
					String pt = p.publishtagetname().length()>0? p.publishtagetname(): null;
					if(pt==null && !p.publishtarget().equals(Object.class))
						pt = SReflect.getClassName(p.publishtarget());
					
					PublishInfo pi = new PublishInfo(p.publishid(), p.publishtype(), pt, Object.class.equals(p.mapping())? null: p.mapping());
					model.addPublishInfo(pi);
				}
			}
			
			cma = cma.getSuperclass();
		}
				
		return model;
	}
}
