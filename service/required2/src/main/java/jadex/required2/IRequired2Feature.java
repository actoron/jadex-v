package jadex.required2;

import java.util.Collection;

import jadex.core.IComponentFeature;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.provided2.impl.search.ServiceQuery;

/**
 *  Required service feature allows to inject, search and query services.
 */
public interface IRequired2Feature extends IComponentFeature
{
	//-------- methods for local lookup --------
	
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
	 *  @param query	The search query.
	 *  @return A collection of services.
	 */
	public <T> Collection<T> getLocalServices(ServiceQuery<T> query);
	
	//-------- methods for remote searching --------
		
	/**
	 *  Search for matching services and provide first result.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> IFuture<T> searchService(ServiceQuery<T> query);
	
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
	 *  @param name The name of the required service declaration.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query);
}
