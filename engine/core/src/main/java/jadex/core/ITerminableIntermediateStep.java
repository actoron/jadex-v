package jadex.core;

import jadex.future.ITerminableIntermediateFuture;

/**
 *  Marker interface for scheduling terminable steps with intermediate results. 
 */
public interface ITerminableIntermediateStep<R>	extends IThrowingFunction<IComponent, ITerminableIntermediateFuture<R>>, IStep
{
	@Override
	public ITerminableIntermediateFuture<R>	apply(IComponent t) throws Exception;
}
