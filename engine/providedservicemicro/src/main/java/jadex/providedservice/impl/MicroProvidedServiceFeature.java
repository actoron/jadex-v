package jadex.providedservice.impl;

import jadex.core.impl.Component;
import jadex.micro.MicroAgent;
import jadex.model.IModelFeature;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.impl.service.ProvidedServiceFeature;
import jadex.providedservice.impl.service.ProvidedServiceModel;

public class MicroProvidedServiceFeature extends ProvidedServiceFeature
{
	protected MicroProvidedServiceFeature(Component self)
	{
		super(self);
	}
	
	public ProvidedServiceModel loadModel()
	{
		ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();

		ProvidedServiceModel mymodel = (ProvidedServiceModel)model.getFeatureModel(IProvidedServiceFeature.class);
		if(mymodel==null)
		{
			mymodel = (ProvidedServiceModel)ProvidedServiceLoader.readFeatureModel(((MicroAgent)self).getPojo().getClass(), this.getClass().getClassLoader());
			final ProvidedServiceModel fmymodel = mymodel;
			AbstractModelLoader loader = AbstractModelLoader.getLoader((Class< ? extends Component>)self.getClass());
			loader.updateCachedModel(() ->
			{
				model.putFeatureModel(IProvidedServiceFeature.class, fmymodel);
			});
		}
		
		return mymodel;
	}
	
	/**
	 *  Create a basic invocation handler for a provided service.
	 * /
	public AbstractServiceInvocationHandler createProvidedHandler(String name, Component ia, Class<?> type, Object service, ProvidedServiceInfo info)
	{
//		if(type.getName().indexOf("ITestService")!=-1 && ia.getComponentIdentifier().getName().startsWith("Global"))
//			System.out.println("gaga");
		
		Map<String, Object> serprops = new HashMap<String, Object>();
		if(info != null && info.getProperties() != null)
		{
			for(UnparsedExpression exp : info.getProperties())
			{
				Object val = SJavaParser.parseExpression(exp, ia.getFeature(IModelFeature.class).getModel().getAllImports(), ia.getClassLoader()).getValue(ia.getFeature(IModelFeature.class).getFetcher());
				serprops.put(exp.getName(), val);
			}
		}
		
		AbstractServiceInvocationHandler handler;
		if(service instanceof IService)
		{
			IService ser = (IService)service;
			
			if(service instanceof BasicService)
			{
				//serprops.putAll(((BasicService)service).getPropertyMap());
				//((BasicService)service).setPropertyMap(serprops);
			}
			
			handler = new ServiceInvocationHandler(ia, ser, false);
			
//			if(type==null)
//			{
//				type = ser.getServiceIdentifier().getServiceType();
//			}
//			else if(!type.equals(ser.getServiceIdentifier().getServiceType()))
//			{
//				throw new RuntimeException("Service does not match its type: "+type+", "+ser.getServiceIdentifier().getServiceType());
//			}
		}
		else
		{
			if(type==null)
			{
				// Try to find service interface via annotation
				if(service.getClass().isAnnotationPresent(Service.class))
				{
					Service si = (Service)service.getClass().getAnnotation(Service.class);
					if(!si.value().equals(Object.class))
					{
						type = si.value();
					}
				}
				// Otherwise take interface if there is only one
				else
				{
					Class<?>[] types = service.getClass().getInterfaces();
					if(types.length!=1)
						throw new RuntimeException("Unknown service interface: "+SUtil.arrayToString(types));
					type = types[0];
				}
			}
			
			Class<?> serclass = service.getClass();

			BasicService mgmntservice = new BasicService(ia.getId(), type, serclass, null);
			mgmntservice.setServiceIdentifier(BasicService.createServiceIdentifier(ia, name, type, service.getClass(), info));
			//mgmntservice.setServiceIdentifier(UUID.randomUUID());
			//serprops.putAll(mgmntservice.getPropertyMap());
			mgmntservice.setPropertyMap(serprops);
			
			// Do not try to call isAnnotationPresent for Proxy on Android
			// see http://code.google.com/p/android/issues/detail?id=24846
			if(!(ProxyFactory.isProxyClass(serclass)))
			//if(!(SReflect.isAndroid() && ProxyFactory.isProxyClass(serclass)))
			{
				while(!Object.class.equals(serclass))
				{
					Field[] fields = serclass.getDeclaredFields();
					for(int i=0; i<fields.length; i++)
					{
						if(fields[i].isAnnotationPresent(ServiceIdentifier.class))
						{
							ServiceIdentifier si = (ServiceIdentifier)fields[i].getAnnotation(ServiceIdentifier.class);
							if(si.value().equals(Object.class) || si.value().equals(type))
							{
								if(SReflect.isSupertype(UUID.class, fields[i].getType()))
								{
									try
									{
										SAccess.setAccessible(fields[i], true);
										fields[i].set(service, mgmntservice.getServiceId());
									}
									catch(Exception e)
									{
										e.printStackTrace();
									}
								}
								else
								{
									throw new RuntimeException("Field cannot store IServiceIdentifer: "+fields[i]);
								}
							}
						}
						
						if(fields[i].isAnnotationPresent(ServiceComponent.class))
						{
							try
							{								
								Object val	= ia.getFeature(IModelFeature.class).getParameterGuesser().guessParameter(fields[i].getType(), false);
								SAccess.setAccessible(fields[i], true);
								fields[i].set(service, val);
							}
							catch(Exception e)
							{
//								e.printStackTrace();
								throw new RuntimeException(e);
							}
						}
					}
					serclass = serclass.getSuperclass();
				}
			}
			
			ServiceInfo si = new ServiceInfo(service, mgmntservice);
			handler = new ServiceInvocationHandler(ia, si);//, ia.getDescription().getCause());
			
//			addPojoServiceIdentifier(service, mgmntservice.getServiceIdentifier());
		}
		
		return handler;
	}*/
}
