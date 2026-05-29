package jadex.core;

import jadex.future.IPullSubscriptionIntermediateFuture;

/**
 *  Marker interface for scheduling steps with pull subscription results.
 */
public interface IPullSubscriptionStep<R>	extends IThrowingFunction<IComponent, IPullSubscriptionIntermediateFuture<R>>, IStep
{
	@Override
	public IPullSubscriptionIntermediateFuture<R>	apply(IComponent t) throws Exception;
}
