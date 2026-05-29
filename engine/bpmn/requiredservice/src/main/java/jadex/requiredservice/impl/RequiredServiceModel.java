package jadex.requiredservice.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jadex.collection.MultiCollection;
import jadex.common.SUtil;

public class RequiredServiceModel
{
	protected Map<String, RequiredServiceInfo> services = new HashMap<String, RequiredServiceInfo>();
	
	protected MultiCollection<String, ServiceInjectionInfo> serviceinjections;

	public Map<String, RequiredServiceInfo> getRequiredServices() 
	{
		return services;
	}

	public void setRequiredServices(Map<String, RequiredServiceInfo> services) 
	{
		this.services = services;
	}
	
	public void addRequiredService(RequiredServiceInfo info)
	{
		addRequiredService(info.getName(), info);
	}
	
	public void addRequiredService(String name, RequiredServiceInfo info)
	{
		if(name==null)
			throw new RuntimeException("Name must not null");
		this.services.put(name, info);
	}
	
	/**
	 *  Get the required service.
	 *  @return The required service.
	 */
	public RequiredServiceInfo getService(String name)
	{
		return services!=null? services.get(name): null;
	}
	
	/**
	 *  Add an injection field.
	 *  @param name The name.
	 *  @param field The field. 
	 */
	public void addServiceInjection(String name, ServiceInjectionInfo si)
	{
		if(serviceinjections==null)
			serviceinjections = new MultiCollection<String, ServiceInjectionInfo>();
		serviceinjections.add(name, si);
	}
	
	/**
	 *  Add an injection field.
	 *  @param name The name.
	 *  @param field The field. 
	 * /
	public void addServiceInjection(String name, FieldInfo field, boolean lazy, boolean query)
	{
		if(serviceinjections==null)
			serviceinjections = new MultiCollection<String, ServiceInjectionInfo>();
		serviceinjections.add(name, new ServiceInjectionInfo(field, lazy, query));
	}*/
	
	/**
	 *  Add an injection method.
	 *  @param name The name.
	 *  @param method The method. 
	 * /
	public void addServiceInjection(String name, MethodInfo method)
	{
		if(serviceinjections==null)
			serviceinjections = new MultiCollection<String, ServiceInjectionInfo>();
		serviceinjections.add(name, new ServiceInjectionInfo(method, false));
	}*/
	
	/**
	 *  Add an injection field.
	 *  @param name The name.
	 *  @param method The method. 
	 * /
	public void addServiceInjection(String name, MethodInfo method, boolean query)
	{
		if(serviceinjections==null)
			serviceinjections = new MultiCollection<String, ServiceInjectionInfo>();
		serviceinjections.add(name, new ServiceInjectionInfo(method, query));
	}*/
	
	/**
	 *  Get the service injection fields.
	 *  @return The field or method infos.
	 */
	public ServiceInjectionInfo[] getServiceInjections(String name)
	{
		Collection<ServiceInjectionInfo> col = serviceinjections==null? null: serviceinjections.get(name);
		return col==null? new ServiceInjectionInfo[0]: (ServiceInjectionInfo[])col.toArray(new ServiceInjectionInfo[col.size()]);
	}
	
	/**
	 *  Get the service injection names.
	 *  @return The names.
	 */
	public String[] getServiceInjectionNames()
	{
		return serviceinjections==null? SUtil.EMPTY_STRING_ARRAY: 
			(String[])serviceinjections.keySet().toArray(new String[serviceinjections.size()]);
	}
}