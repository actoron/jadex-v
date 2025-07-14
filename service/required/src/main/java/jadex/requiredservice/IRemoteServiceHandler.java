package jadex.requiredservice;

import java.util.Set;

import jadex.core.IComponent;
import jadex.core.annotation.NoCopy;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.search.ServiceQuery;

@Service
public interface IRemoteServiceHandler
{
	/**
	 *  Get a remote service proxy.
	 *  @param sid The service id.
	 *  @return The service.
	 */
	// Must NOT clone the component and service
	public @NoCopy IFuture<IService> getRemoteServiceProxy(@NoCopy IComponent comp, IServiceIdentifier sid);
	
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
	
	/**
	 *  Get the security group names.
	 *  @return The security group names.
	 */
	public Set<String> getGroupNames();
}
