package jadex.providedservice.impl.service;

import java.util.HashMap;
import java.util.Map;

public class ProvidedServiceModel 
{
	protected Map<String, ProvidedServiceInfo> services;
	
	public ProvidedServiceModel()
	{
	}
	
	public ProvidedServiceInfo[] getServices()
	{
		return services==null? new ProvidedServiceInfo[0]: services.values().toArray(new ProvidedServiceInfo[services.size()]);
	}
	
	public void addService(ProvidedServiceInfo service)
	{
		if(services==null)
			services = new HashMap<String, ProvidedServiceInfo>();
		if(service.getName()==null)
			throw new RuntimeException("provided service should have name");
		services.put(service.getName(), service);
	}
	
	/*public void addService(String name, ProvidedServiceInfo service)
	{
		if(services==null)
			services = new HashMap<String, ProvidedServiceInfo>();
		services.put(name, service);
	}*/
	
	public ProvidedServiceInfo getService(String name)
	{
		return services==null? null: services.get(name);
	}
	
	public boolean hasService(String name)
	{
		return services==null? false: services.containsKey(name);
	}
}

