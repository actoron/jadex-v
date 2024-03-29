package jadex.requiredservice.impl;

import java.util.Collection;

import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.requiredservice.RequiredServiceInfo;

/**
 *  Interface for fetching required services.
 */
public interface IRequiredServiceFetcher
{
	/**
	 *  Get a required service.
	 *  @param info The service info.
	 *  @param provider The provider.
	 *  @param rebind Flag if should be rebound.
	 */
	public <T> IFuture<T> getService(RequiredServiceInfo info);
	
	/**
	 *  Get a required multi service.
	 *  @param info The service info.
	 *  @param provider The provider.
	 *  @param rebind Flag if should be rebound.
	 */
	public <T> ITerminableIntermediateFuture<T> getServices(RequiredServiceInfo info);

	/**
	 *  Get the result of the last search.
	 */
	public <T> T getLastService();

	/**
	 *  Get the result of the last search.
	 */
	public <T> Collection<T> getLastServices();

}
