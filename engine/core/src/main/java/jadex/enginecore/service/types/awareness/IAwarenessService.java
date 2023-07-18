package jadex.enginecore.service.types.awareness;

import java.util.List;

import jadex.enginecore.IComponentIdentifier;
import jadex.enginecore.service.annotation.Service;
import jadex.enginecore.service.types.address.TransportAddress;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;

/**
 *  Locate other platforms without polling.
 */
@Service(system=true)
public interface IAwarenessService
{
	/**
	 *  Try to find other platforms and finish after timeout.
	 *  Immediately returns known platforms and concurrently issues a new search, waiting for replies until the timeout.
	 */
	public IIntermediateFuture<IComponentIdentifier> searchPlatforms();
	
	/**
	 *  Try to find other platforms while providing a quick answer.
	 *  Services should respond to a call as close to instantaneous as possible, but
	 *  with an upper bound of less than 1 second.
	 *  Issues a new search, but answers using known platforms. On first request
	 */
//	public IFuture<Set<IComponentIdentifier>> searchPlatformsFast();
	
	/**
	 *  Gets the address for a platform ID using the awareness mechanism.
	 * 
	 *  @param platformid The platform ID.
	 *  @return The transport addresses or null if not available.
	 */
	public IFuture<List<TransportAddress>> getPlatformAddresses(IComponentIdentifier platformid);
	
//	/**
//	 *  Immediately return known platforms and continuously publish newly found platforms.
//	 *  Does no active searching.
//	 */
//	// currently unused.
//	public ISubscriptionIntermediateFuture<IComponentIdentifier>	subscribeToNewPlatforms();
}
