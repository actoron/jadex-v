package jadex.enginecore.service.types.registry;

import jadex.enginecore.service.IServiceIdentifier;
import jadex.enginecore.service.annotation.Service;
import jadex.enginecore.service.search.ServiceQuery;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.ITerminableIntermediateFuture;

/**
 *  Local service for handling all remote searches and queries
 *  including superpeer management and plain polling search fallback.
 */
@Service(system=true)
public interface ISearchQueryManagerService
{
	/**
	 *  Search for matching services using available remote information sources and provide first result.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> ITerminableFuture<IServiceIdentifier> searchService(ServiceQuery<T> query);
	
	/**
	 *  Search for all matching services.
	 *  @param query	The search query.
	 *  @return Each service as an intermediate result or a collection of services as final result.
	 */
	public <T> ITerminableIntermediateFuture<IServiceIdentifier> searchServices(ServiceQuery<T> query);
	
	/**
	 *  Add a service query.
	 *  Continuously searches for matching services using available remote information sources.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query);
}
