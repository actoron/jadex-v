package jadex.registry.coordinator;

import jadex.future.ISubscriptionIntermediateFuture;
import jadex.providedservice.annotation.Service;
import jakarta.ws.rs.GET;

@Service
public interface ICoordinatorGuiService 
{
	/**
	 *  Subscribe to coordinator updates.
	 */
	@GET
	public ISubscriptionIntermediateFuture<CoordinatorServiceEvent> subscribe();
	
}
