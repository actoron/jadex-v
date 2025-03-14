package jadex.provided2.impl.search;

import jadex.provided2.IServiceIdentifier;

/**
 *  Service event used if the service registry is used in event mode.
 */
public class ServiceEvent
{
	/** Service was added event. */
	public static final int SERVICE_ADDED = 0;
	
	/** Service was removed event. */
	public static final int SERVICE_REMOVED = 1;
	
	/** Service changed. */
	public static final int SERVICE_CHANGED = 2;
	
	/** Event type. */
	protected int type;
	
	/** The service. */
	protected IServiceIdentifier service;
	
	/** Bean constructor. */
	public ServiceEvent()
	{
	}
	
	/**
	 *  Creates the service event.
	 *  @param service The affected service.
	 *  @param eventtype The event type.
	 */
	public ServiceEvent(IServiceIdentifier service, int eventtype)
	{
		// todo: refactor to not using a changing type of T (service id and service)
		// service event is created with service identifier
		// processServiceEvent in RequiredServiceComponentFeature converts it to a service proxy
		
		this.service = service;
		this.type = eventtype;
		
		//if(eventtype==SERVICE_REMOVED && service instanceof IService)
		//	System.out.println("here");
	}

	/**
	 *  Gets the event type.
	 *
	 *  @return The event type.
	 */
	public int getType()
	{
		return type;
	}

	/**
	 *  Sets the event type.
	 *
	 *  @param eventtype The event type.
	 */
	public void setType(int type)
	{
		this.type = type;
	}

	/**
	 *  Gets the service.
	 *
	 *  @return The service.
	 */
	public IServiceIdentifier getService()
	{
		return service;
	}

	/**
	 *  Sets the service.
	 *
	 *  @param service The service.
	 */
	public void setService(IServiceIdentifier service)
	{
		this.service = service;
		//if(this.type==SERVICE_REMOVED && service instanceof IService)
		//	System.out.println("here");
	}

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		return "ServiceEvent [type=" + type + ", service=" + service + "]";
	}
}
