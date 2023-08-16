package jadex.enginecore.service.types.remote;

import jadex.enginecore.IComponentIdentifier;
import jadex.enginecore.service.annotation.Service;
import jadex.future.IFuture;

/**
 *  Service for administration of proxy agents.
 */
@Service
public interface IProxyAgentService
{
	/** The connection state. */
	public static enum State
	{
		CONNECTED,
		UNCONNECTED,
		LOCKED
	}
	
	/**
	 *  Get the component identifier of the remote platform.
	 */
	public IFuture<IComponentIdentifier>	getRemoteComponentIdentifier();

	/**
	 *  Set or update the component identifier of the remote platform,
	 *  i.e., top reflect new transport addresses.
	 */
	public IFuture<Void>	setRemoteComponentIdentifier(IComponentIdentifier cid);
	
	/**	
	 *  Get the connection state of the proxy.
	 *  @return The connection state.
	 */
	public IFuture<State> getConnectionState();

	/**
	 *  Refresh the latency value.
	 */
	public IFuture<Void>	refreshLatency();
}
