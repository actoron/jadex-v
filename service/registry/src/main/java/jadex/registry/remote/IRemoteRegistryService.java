package jadex.registry.remote;

import java.util.Set;

import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.search.ServiceQuery;

//@Security(roles=Security.UNRESTRICTED)	// Allow invocation and check in impl.
@Service
public interface IRemoteRegistryService
{
	/** Name of the remote registry component and service. */
	//public static final String REMOTE_REGISTRY_NAME = "remoteregistry";
	
	/**
	 *  Search remote registry for a single service.
	 *  
	 *  @param query The search query.
	 *  @return The first matching service or null if not found.
	 */
	public IFuture<IServiceIdentifier> searchService(ServiceQuery<?> query);
	
	/**
	 *  Search remote registry for services.
	 *  
	 *  @param query The search query.
	 *  @return The matching services or empty set if none are found.
	 */
	public IFuture<Set<IServiceIdentifier>> searchServices(ServiceQuery<?> query);
	
	/**
	 *  Initiates the client registration procedure
	 *  (super peer will answer initially with an empty intermediate result,
	 *  client will send updates with backward commands).
	 *  
	 *  @param networkname	Network for this connection. 
	 *  
	 *  @return Does not return any more results while connection is running.
	 */
	// TODO: replace internal commands with typed channel (i.e. bidirectional / reverse subscription future), first step terminable tuple2 future?
	// TODO: network name required for server?
	public ISubscriptionIntermediateFuture<Void> registerClient();//String networkname);
	
	/**
	 *  Add a service query to the registry.
	 *  
	 *  @param query The service query.
	 *  @return Subscription to matching services.
	 */
	public <T> ISubscriptionIntermediateFuture<Object> addQuery(ServiceQuery<T> query);
}