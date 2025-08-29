package jadex.registry.remote;

import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.search.QueryEvent;
import jadex.providedservice.impl.search.ServiceEvent;
import jadex.providedservice.impl.search.ServiceQuery;

public class RegistryEvent 
{
	private IServiceIdentifier serviceIdentifier;
    private ServiceQuery<?> query;
    private int type;

	public RegistryEvent()
	{
	}

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

	public RegistryEvent(ServiceEvent event) 
	{
		this(event!=null? event.getService(): null, event!=null? event.getType(): -1);
	}

	public RegistryEvent(QueryEvent event) 
	{
		this(event!=null? event.getQuery(): null, event!=null? event.getType(): -1);
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

	public void setQuery(ServiceQuery<?> query) 
	{
		this.query = query;
	}
	
	public void setService(IServiceIdentifier serviceIdentifier) 
	{
		this.serviceIdentifier = serviceIdentifier;
	}

	public void setType(int type) 
	{
		this.type = type;
	}

	@Override
	public String toString() 
	{
		return "RegistryEvent [serviceIdentifier=" + serviceIdentifier + ", query=" + query + ", type=" + type + "]";
	}
   
}