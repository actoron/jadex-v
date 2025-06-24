package jadex.execution.impl;

import jadex.core.impl.Component;
import jadex.future.Future;

/**
 *  Marker class for Lambda Agent optimizations when started with run(),
 *  .i.e. single step execution of init/run/terminate.
 */
public class FastLambda<T>	extends Component
{
	/** The result, if any. */
	protected Future<T>	result;
	
	/** Terminate immediately after body. */
	// Set to false for memory benchmarking
	protected boolean	terminate;
	
	public FastLambda(Object body, Future<T> result, boolean terminate)
	{
		super(body);
		this.terminate	= terminate;
		this.result	= result;
	}
}
