package jadex.providedservice.impl.service;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadex.common.SReflect;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.impl.ILifecycle;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.InjectionFeature;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.ProvideService;
import jadex.providedservice.impl.search.ServiceRegistry;
import jadex.providedservice.impl.service.interceptors.DecouplingInterceptor;
import jadex.providedservice.impl.service.interceptors.DecouplingReturnInterceptor;
import jadex.providedservice.impl.service.interceptors.MethodInvocationInterceptor;

/**
 *  Component feature that handles detection and registration of provided services.
 */
public class ProvidedServiceFeature implements IProvidedServiceFeature, ILifecycle
{
	/** The component. */
	protected IComponent	self;
	
	protected Map<Object, List<IService>>	services;
	
	/**
	 *  Create the injection feature.
	 */
	public ProvidedServiceFeature(IComponent self)
	{
		this.self	= self;
	}
	
	@Override
	public <T> T getProvidedService(Class<T> type)
	{
		if(services!=null)
		{
			for(List<IService> lservices: services.values())
			{
				for(IService service: lservices)
				{
					if(SReflect.isSupertype(type, service.getClass()))
					{
						@SuppressWarnings("unchecked")
						T	ret	= (T)service;
						return ret;
					}
				}
			}
		}
		return null;
	}
	
	//-------- ILifecycle methods --------
	
	/**
	 *  Called in order of features, after all features are instantiated.
	 */
	public void	onStart()
	{
		// NOP -> injection is done by extending injection feature in Provided2FeatureProvider
	}
	
	/**
	 *  Called in reverse order of features, when the component terminates.
	 */
	public void	onEnd()
	{
		if(services!=null)
		{
			for(List<IService> lservices: services.values())
			{
				for(IService service: lservices)
				{
//					System.out.println("remove: "+service);
					ServiceRegistry.getRegistry().removeService(service.getServiceId());
				}
			}
		}
	}

	//-------- internal methods --------

	/**
	 *  Register a service.
	 */
	protected void	addService(Object pojo, String name, Map<Class<?>, ProvideService> interfaces)
	{
		if(services==null)
		{
			services	= new LinkedHashMap<>();
		}
		
		// May be added already due to first field service found and then service interface found again as extra object.
		if(!services.containsKey(pojo))
		{
			// Need to add it here to avoid recursive adddExtraObject for pojo
			List<IService>	lservices	= new ArrayList<>(1);
			services.put(pojo, lservices);
			
			// If service pojo is not the component pojo -> handle injection in service pojo as well
			if(pojo!=self.getPojo())
			{
				List<Object>	pojos	= Arrays.asList(new Object[]{self.getPojo(), pojo});	// TODO: recursive sub-services
				((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(pojos);
			}
			
			// Provide all implemented service interfaces.
			for(Class<?> type: interfaces.keySet())
			{
				// TODO: name given (i.e. field/method) and multiple interfaces
				String	thename	= name!=null ? name : type.getSimpleName();  
				IService	service	= createProvidedServiceProxy(pojo, thename, type);
				ServiceRegistry.getRegistry().addLocalService(service);
				lservices.add(service);
			}
		}
	}

	/**
	 *  Create a standard service proxy for a provided service.
	 */
	protected IService	createProvidedServiceProxy(Object service, String name, Class<?> type)
	{
		// Create proxy with handler
		IServiceIdentifier	sid	= new ServiceIdentifier(self, type, name, null, false, null);
		ServiceInvocationHandler handler	= new ServiceInvocationHandler(sid, service);		
		IService	ret	= (IService)Proxy.newProxyInstance(IComponentManager.get().getClassLoader(), new Class[]{IService.class, type}, handler);
		
		// Add interceptors to handler
		handler.addFirstServiceInterceptor(new MethodInvocationInterceptor());
		handler.addFirstServiceInterceptor(new DecouplingInterceptor(self));
		handler.addFirstServiceInterceptor(new DecouplingReturnInterceptor());
		
		return ret;
	}
}
