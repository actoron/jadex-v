package jadex.core;

import jadex.future.ITerminableFuture;

/**
 *  Marker interface for scheduling terminable steps.
 */
public interface ITerminableStep<R>	extends IThrowingFunction<IComponent, ITerminableFuture<R>>, IStep
{
	@Override
	public ITerminableFuture<R>	apply(IComponent t) throws Exception;
}
