package jadex.core;

import jadex.future.IIntermediateFuture;

/**
 *  Marker interface for scheduling steps with intermediate results.
 */
public interface IIntermediateStep<R>	extends IThrowingFunction<IComponent, IIntermediateFuture<R>>, IStep
{
	@Override
	public IIntermediateFuture<R>	apply(IComponent t) throws Exception;
}
