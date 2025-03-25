package jadex.providedservice.impl.search;

import jadex.future.TerminableIntermediateFuture;

/**
 *  Info with query and result future.
 */
public class ServiceQueryInfo<T>
{
	/** The query. */
	protected ServiceQuery<T> query;
	
	/** The futures. */
	protected TerminableIntermediateFuture<Object> future;

	/**
	 *  Create a new query info.
	 */
	public ServiceQueryInfo(ServiceQuery<T> query, TerminableIntermediateFuture<Object> future)
	{
		this.query = query;
		this.future = future;
	}

	/**
	 *  Get the query.
	 *  @return The query
	 */
	public ServiceQuery<T> getQuery()
	{
		return query;
	}

	/**
	 *  Get the future.
	 *  @return The future
	 */
	public TerminableIntermediateFuture<Object> getFuture()
	{
		return future;
	}
}