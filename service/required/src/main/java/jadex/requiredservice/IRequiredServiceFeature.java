package jadex.requiredservice;

import java.util.Collection;

import jadex.core.IComponentFeature;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.providedservice.impl.search.ServiceQuery;

/**
 *  Required service feature allows to inject, search and query services.
 */
public interface IRequiredServiceFeature extends IComponentFeature
{
	//-------- methods for local lookup --------
	
	/**
	 *  Lookup matching services and provide first result.
	 *  Synchronous method only for locally available services.
	 *  @param type	The service type.
	 *  @return The corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> T getLocalService(Class<T> type);
	
	/**
	 *  Lookup matching services and provide first result.
	 *  Synchronous method only for locally available services.
	 *  @param query	The search query.
	 *  @return The corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> T getLocalService(ServiceQuery<T> query);
	
	/**
	 *  Lookup all matching services.
	 *  Synchronous method only for locally available services.
	 *  @param type	The service type
	 *  @return A collection of services.
	 */
	public <T> Collection<T> getLocalServices(Class<T> type);
	
	/**
	 *  Lookup all matching services.
	 *  Synchronous method only for locally available services.
	 *  @param query	The search query.
	 *  @return A collection of services.
	 */
	public <T> Collection<T> getLocalServices(ServiceQuery<T> query);
	
	//-------- methods for remote searching --------
		
	/**
	 *  Search for matching services and provide first result.
	 *  @param type	The service type-
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> IFuture<T> searchService(Class<T> type);
			
	/**
	 *  Search for matching services and provide first result.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> IFuture<T> searchService(ServiceQuery<T> query);
	
	/**
	 *  Search for all matching services.
	 *  @param type	The service type.
	 *  @return Each service as an intermediate result or a collection of services as final result.
	 */
	public <T> ITerminableIntermediateFuture<T> searchServices(Class<T> type);
	
	/**
	 *  Search for all matching services.
	 *  @param query	The search query.
	 *  @return Each service as an intermediate result or a collection of services as final result.
	 */
	public <T> ITerminableIntermediateFuture<T> searchServices(ServiceQuery<T> query);
		
	//-------- methods for remote querying --------
	
	/**
	 *  Add a query for a declared required service.
	 *  Continuously searches for matching services.
	 *  @param type	The service type.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(Class<T> type);
	
	/**
	 *  Add a query for a declared required service.
	 *  Continuously searches for matching services.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query);
}
