package jadex.core;

import jadex.future.ISubscriptionIntermediateFuture;

/**
 *  Marker interface for scheduling steps with subscription results.
 */
public interface ISubscriptionStep<R>	extends IThrowingFunction<IComponent, ISubscriptionIntermediateFuture<R>>, IStep
{
	@Override
	public ISubscriptionIntermediateFuture<R>	apply(IComponent t) throws Exception;
}
