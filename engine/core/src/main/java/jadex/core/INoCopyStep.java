package jadex.core;

import jadex.core.annotation.NoCopy;

/**
 *  Marker interface for scheduling component steps as lambda,
 *  but without copying result values.
 */
public interface INoCopyStep<R>	extends IThrowingFunction<IComponent, R>, IStep
{
	@Override
	public @NoCopy R	apply(IComponent t) throws Exception;
}
