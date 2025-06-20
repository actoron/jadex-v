package jadex.providedservice.impl.search;

import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import jadex.core.ComponentIdentifier;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;

/**
 *  Interface for a service registry.
 *  
 *  todo: further cleanup the interface
 */
public interface IServiceRegistry
{
	/**
	 *  Search for a service.
	 */
	public IServiceIdentifier searchService(ServiceQuery<?> query);
	
	/**
	 *  Search for services.
	 */
	public Set<IServiceIdentifier> searchServices(ServiceQuery<?> query);
	
	/**
	 *  Add a service to the registry.
	 *  @param service The service.
	 */
	// write
	public void addService(IServiceIdentifier service);
	
	/**
	 *  Add a local service to the registry.
	 *  @param service The local service.
	 */
	// write
	public void addLocalService(IService service);
	
	/**
	 *  Updates a service if the service meta-information
	 *  has changes.
	 *  
	 *  @param service The service.
	 */
	public void updateService(IServiceIdentifier service);
	
	/**
	 *  Remove a service from the registry.
	 *  @param service The service.
	 */
	// write
	public void removeService(IServiceIdentifier service);
	
	/**
	 *  Remove services of a platform from the registry.
	 *  @param platform The platform, null for everything.
	 */
	// write
	public void removeServices(ComponentIdentifier runtime);
	
	/**
	 *  Remove services except from a platform from the registry.
	 *  @param platform The platform.
	 */
	// write
	//public void removeServicesExcept(IComponentIdentifier platform);
	
	/**
	 *  Add a service query to the registry.
	 *  @param query ServiceQuery.
	 *  @return Found services in form of service identifier or service events when query is in event mode.
	 */
	// write
	public <T> ISubscriptionIntermediateFuture<Object> addQuery(ServiceQuery<T> query);
	
	/**
	 *  Remove a service query from the registry.
	 *  @param query ServiceQuery.
	 */
	// write
	public void removeQuery(final ServiceQuery<?> query);
	
	/**
	 *  Remove all service queries of a specific component from the registry.
	 *  @param owner The query owner.
	 */
	// write
	public void removeQueries(ComponentIdentifier owner);
	
	/**
	 *  Remove all service queries of a specific platform from the registry.
	 *  @param platform The platform from which the query owner comes.
	 */
	// write
	public void removeQueriesOfRuntime(ComponentIdentifier runtime);
	
	/** 
	 *  Returns the service proxy of a local service identified by service ID.
	 *  
	 *  @param serviceid The service ID.
	 *  @return The service proxy.
	 */
	public IService getLocalService(IServiceIdentifier serviceid);
	
	/**
	 *  Get all services.
	 *  @return All services (copy).
	 */
	public Set<IServiceIdentifier> getAllServices();
	
	/**
	 *  Get all queries.
	 *  @return All queries (copy).
	 */
	public Set<ServiceQueryInfo<?>> getAllQueries();
	
	/**
	 *  Subscribe for query events.
	 */
	public ISubscriptionIntermediateFuture<QueryEvent> subscribeToQueries();
	
	/**
	 *  Returns the lock on the registry.
	 *  Care must be taken to perform proper unlocking
	 *  to avoid permanently blocking the registry.
	 *  Note that the lock is reentrant, so operations
	 *  can be performed while the lock is held.
	 *  
	 *  @return The registry lock.
	 */
	public ReadWriteLock getLock();
}
