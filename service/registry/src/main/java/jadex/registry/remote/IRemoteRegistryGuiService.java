package jadex.registry.remote;


import jadex.future.ISubscriptionIntermediateFuture;
import jadex.providedservice.annotation.Service;
import jakarta.ws.rs.GET;

@Service
public interface IRemoteRegistryGuiService
{
	/**
	 *  Subscribe to coordinator updates.
	 */
	@GET
	public ISubscriptionIntermediateFuture<RegistryEvent> subscribe();
	
}
