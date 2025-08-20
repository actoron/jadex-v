package jadex.registry.remote;

import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.search.ServiceQuery;

public class RegistryEvent 
{
	private IServiceIdentifier serviceIdentifier;
    private ServiceQuery<?> query;
    private int type;

    public RegistryEvent(IServiceIdentifier serviceIdentifier, int type) 
	{
		this.serviceIdentifier = serviceIdentifier;
		this.type = type;
	}
    
    public RegistryEvent(ServiceQuery<?> query, int type) 
    {
    	this.query = query;
    	this.type = type;
	}
	
	public IServiceIdentifier getService() 
	{
		return serviceIdentifier;
	}
	
	public ServiceQuery<?> getQuery() 
	{
		return query;
	}
	
	public int getType() 
	{
		return type;
	}
   
}