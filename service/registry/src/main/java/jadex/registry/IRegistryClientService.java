package jadex.registry;

import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRemoteServiceHandler;

/**
 *  Local service for handling all remote searches and queries.
 */
@Service
public interface IRegistryClientService extends IRemoteServiceHandler
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
	 *  Add a service query. Remove is done by calling the returned future's terminate method. 
	 *  Continuously searches for matching services using available remote information sources.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query);
	
	/**
	 *  Add a coordinator service name to the list of known coordinators.
	 *  @param name	The coordinator service name.
	 * /
	public void addCoordinatorServiceName(String name);*/
	
}