package jadex.provided2.impl;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.SReflect;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.impl.ILifecycle;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.InjectionFeature;
import jadex.provided2.IProvided2Feature;
import jadex.provided2.IService;
import jadex.provided2.IServiceIdentifier;
import jadex.provided2.impl.interceptors.DecouplingInterceptor;
import jadex.provided2.impl.interceptors.DecouplingReturnInterceptor;
import jadex.provided2.impl.interceptors.MethodInvocationInterceptor;
import jadex.provided2.impl.search.ServiceRegistry;

/**
 *  Component feature that handles detection and registration of provided services.
 */
public class Provided2Feature implements IProvided2Feature, ILifecycle
{
	/** The component. */
	protected IComponent	self;
	
	protected Map<Object, IService>	services;
	
	/**
	 *  Create the injection feature.
	 */
	public Provided2Feature(IComponent self)
	{
		this.self	= self;
	}
	
	@Override
	public <T> T getProvidedService(Class<T> type)
	{
		if(services!=null)
		{
			for(IService service: services.values())
			{
				if(SReflect.isSupertype(type, service.getClass()))
				{
					@SuppressWarnings("unchecked")
					T	ret	= (T)service;
					return ret;
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
			for(IService service: services.values())
			{
				ServiceRegistry.getRegistry().removeService(service.getServiceId());
			}
		}
	}

	//-------- internal methods --------

	/**
	 *  Register a service.
	 */
	protected void	addService(Object pojo,  String name, Set<Class<?>> interfaces)
	{
		if(services==null)
		{
			services	= new LinkedHashMap<>();
		}
		
		// May be added already due to first field service found and then service interface found again as extra object.
		if(!services.containsKey(pojo))
		{
			// Need to add it here to avoid recursive adddExtraObject for pojo
			services.put(pojo, null);
			
			// If service pojo is not the component pojo -> handle injection in service pojo as well
			if(pojo!=self.getPojo())
			{
				List<Object>	pojos	= Arrays.asList(new Object[]{self.getPojo(), pojo});	// TODO: recursive sub-services
				((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(pojos);
			}
			
			Class<?>	type	= interfaces.iterator().next();	// Todo multiple types?
			name	= name!=null ? name : type.getSimpleName();  
			IService	service	= createProvidedServiceProxy(pojo, name, type);
			ServiceRegistry.getRegistry().addLocalService(service);
			services.put(pojo, service);
		}
	}

	/**
	 *  Create a standard service proxy for a provided service.
	 */
	protected IService	createProvidedServiceProxy(Object service, String name, Class<?> type)
	{
		// Create proxy with handler
		IServiceIdentifier	sid	= new ServiceIdentifier(self, service.getClass(), name, null, false, null);
		ServiceInvocationHandler handler	= new ServiceInvocationHandler(sid, service);		
		IService	ret	= (IService)Proxy.newProxyInstance(IComponentManager.get().getClassLoader(), new Class[]{IService.class, type}, handler);
		
		// Add interceptors to handler
		handler.addFirstServiceInterceptor(new MethodInvocationInterceptor());
		handler.addFirstServiceInterceptor(new DecouplingInterceptor(self, true, false));
		handler.addFirstServiceInterceptor(new DecouplingReturnInterceptor());
		
		return ret;
	}
}
