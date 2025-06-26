package jadex.registry;

import jadex.future.ISubscriptionIntermediateFuture;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Service;

@Service
public interface IRegistryCoordinatorService 
{
	/** Name of the remote registry component and service. */
	public static final String REGISTRY_COORDINATOR_NAME = "registrycoordinator";
	
	/**
	 *  Initiates the client registration procedure
	 *  (super peer will answer initially with an empty intermediate result,
	 *  client will send updates with backward commands).
	 *  
	 *  @param networkname	Network for this connection. 
	 *  
	 *  @return Does not return any more results while connection is running.
	 */
	public ISubscriptionIntermediateFuture<Void> registerRegistry(IServiceIdentifier reg, long starttime);
	

	public ISubscriptionIntermediateFuture<CoordinatorServiceEvent> getRegistries();

}
