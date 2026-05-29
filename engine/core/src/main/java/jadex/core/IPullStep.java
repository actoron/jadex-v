package jadex.core;

import jadex.future.IPullIntermediateFuture;

/**
 *  Marker interface for scheduling steps with pull results.
 */
public interface IPullStep<R>	extends IThrowingFunction<IComponent, IPullIntermediateFuture<R>>, IStep
{
	@Override
	public IPullIntermediateFuture<R>	apply(IComponent t) throws Exception;
}
