package jadex.provided2.impl;

import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Set;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
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
public class Provided2Feature implements IProvided2Feature
{
	/** The component. */
	protected IComponent	self;
	
	protected Set<Object>	services;
	
	/**
	 *  Create the injection feature.
	 */
	public Provided2Feature(IComponent self)
	{
		this.self	= self;
	}

	/**
	 *  Register a service.
	 */
	protected void	addService(Object pojo,  String name, Set<Class<?>> interfaces)
	{
		if(services==null)
		{
			services	= new LinkedHashSet<>();
		}
		
		// May be added already due to first field service found and then service interface found again as extra object.
		if(!services.contains(pojo))
		{
			services.add(pojo);
			if(pojo!=self.getPojo())
			{
				((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(pojo);
			}
			
			Class<?>	type	= interfaces.iterator().next();	// Todo multiple types?
			name	= name!=null ? name : type.getSimpleName();  
			IService	service	= createProvidedServiceProxy(pojo, name, type);
			ServiceRegistry.getRegistry().addLocalService(service);
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
