package jadex.execution.impl;

import jadex.core.IComponent;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.future.Future;

/**
 *  Marker class for Lambda Agent optimizations when started with run(),
 *  .i.e. single step execution of init/run/terminate.
 */
public class FastLambda<T>	extends Component
{
	/** The body to run. */
	protected IThrowingFunction<IComponent, T>	body;
	
	/** The result, if any. */
	protected Future<T>	result;
	
	/** Terminate immediately after body. */
	// Set to false for memory benchmarking
	protected boolean	terminate;
	
	public FastLambda(IThrowingFunction<IComponent, T> body, Future<T> result, boolean terminate)
	{
		this.body	= body;
		this.terminate	= terminate;
		this.result	= result;
	}
}
