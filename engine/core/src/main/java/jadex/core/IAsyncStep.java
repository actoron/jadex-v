package jadex.core;

import jadex.future.IFuture;

/**
 *  Marker interface for scheduling plain async steps.
 *  Helps to ignore future subtypes, when not needed (e.g. step returns terminable future but outside only needs future).
 */
public interface IAsyncStep<R>	extends IThrowingFunction<IComponent, IFuture<R>>, IStep
{
	@Override
	public IFuture<R>	apply(IComponent t) throws Exception;
}
