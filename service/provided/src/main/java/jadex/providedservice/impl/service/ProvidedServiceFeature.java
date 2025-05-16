package jadex.providedservice.impl.service;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadex.common.MethodInfo;
import jadex.common.SReflect;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.impl.ILifecycle;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.InjectionFeature;
import jadex.providedservice.IMethodInvocationListener;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.ProvideService;
import jadex.providedservice.impl.search.ServiceRegistry;
import jadex.providedservice.impl.service.interceptors.DecouplingInterceptor;
import jadex.providedservice.impl.service.interceptors.DecouplingReturnInterceptor;
import jadex.providedservice.impl.service.interceptors.MethodCallListenerInterceptor;
import jadex.providedservice.impl.service.interceptors.MethodInvocationInterceptor;

/**
 *  Component feature that handles detection and registration of provided services.
 */
public class ProvidedServiceFeature implements IProvidedServiceFeature, ILifecycle
{
	/** The component. */
	protected IComponent	self;
	
	/** Map pojo to provided services of pojo. */
	protected Map<Object, List<IService>>	services;
	
	/** The map of provided service infos. (sid -> method listener) */
	protected Map<IServiceIdentifier, MethodListenerHandler> servicelisteners;
	
	/**
	 *  Create the injection feature.
	 */
	public ProvidedServiceFeature(IComponent self)
	{
		this.self	= self;
	}
	
	//-------- IProvidedServiceFeature interface --------

	@Override
	public <T> T getProvidedService(IServiceIdentifier sid)
	{
		if(services!=null)
		{
			for(List<IService> lservices: services.values())
			{
				for(IService service: lservices)
				{
					if(sid.equals(service.getServiceId()))
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

	//-------- impl methods used by other features --------
	
	/**
	 *  Add a method invocation handler.
	 */
	public void addMethodInvocationListener(IServiceIdentifier sid, MethodInfo mi, IMethodInvocationListener listener)
	{
//		System.out.println("added lis: "+sid+" "+mi+" "+hashCode());
		
		if(servicelisteners==null)
			servicelisteners = new HashMap<IServiceIdentifier, MethodListenerHandler>();
		MethodListenerHandler handler = servicelisteners.get(sid);
		if(handler==null)
		{
			handler = new MethodListenerHandler();
			servicelisteners.put(sid, handler);
		}
		handler.addMethodListener(mi, listener);
	}
	
	/**
	 *  Remove a method invocation handler.
	 */
	public void removeMethodInvocationListener(IServiceIdentifier sid, MethodInfo mi, IMethodInvocationListener listener)
	{
		if(servicelisteners!=null)
		{
			MethodListenerHandler handler = servicelisteners.get(sid);
			if(handler!=null)
			{
				handler.removeMethodListener(mi, listener);
			}
		}
	}
	
	/**
	 *  Notify listeners that a service method has been called.
	 */
	public void notifyMethodListeners(IService service, boolean start, final Method method, final Object[] args, Object callid)
	{
		if(servicelisteners!=null)
		{
			MethodListenerHandler handler = servicelisteners.get(service.getServiceId());
			if(handler!=null)
			{
//				MethodInfo mi = new MethodInfo(method);
				handler.notifyMethodListeners(start, service, method, args, callid);
			}
		}
	}
	
	/**
	 *  Test if service and method has listeners.
	 */
	public boolean hasMethodListeners(IServiceIdentifier sid, MethodInfo mi)
	{
		boolean ret = false;
		if(servicelisteners!=null)
		{
			MethodListenerHandler handler = servicelisteners.get(sid);
			if(handler!=null)
			{
				ret = handler.hasMethodListeners(sid, mi);
			}
		}
		
		return ret;
	}
	
	//-------- internal methods --------

	/**
	 *  Register a service.
	 */
	public void	addService(List<Object> parents, Object pojo, String name, Map<Class<?>, ProvideService> interfaces)
	{
		if(services==null)
		{
			services	= new LinkedHashMap<>();
		}
		
		// May be added already due to first field service found and then service interface found again as extra object.
		if(!services.containsKey(pojo))
		{
			// Need to add it here to avoid recursive adddExtraObject for pojo
			List<IService>	lservices	= new ArrayList<>(interfaces.size());
			services.put(pojo, lservices);
			
			// Create proxies for all implemented service interfaces.
			for(Class<?> type: interfaces.keySet())
			{
				// TODO: name given (i.e. field/method) and multiple interfaces
				String	thename	= name!=null ? name : type.getSimpleName();  
				IService	service	= createProvidedServiceProxy(pojo, thename, type);
				lservices.add(service);
			}
			
			// If service pojo is not the last parent pojo -> handle injection in service pojo as well
			// else last parent directly implements the service
			if(pojo!=parents.get(parents.size()-1))
			{
				List<Object>	pojos	= new ArrayList<>(parents);
				pojos.add(pojo);
				((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(pojos, null, null, null);
			}
			
			// Finally make the services publicly available
			for(IService service: lservices)
			{
				ServiceRegistry.getRegistry().addLocalService(service);
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
		handler.addFirstServiceInterceptor(new MethodCallListenerInterceptor(self, ret));
		handler.addFirstServiceInterceptor(new DecouplingInterceptor(self));
		handler.addFirstServiceInterceptor(new DecouplingReturnInterceptor());
		
		return ret;
	}
}
