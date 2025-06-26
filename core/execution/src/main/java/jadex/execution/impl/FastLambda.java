package jadex.execution.impl;

import jadex.core.impl.Component;
import jadex.future.Future;

/**
 *  Marker class for Lambda Agent optimizations when started with run(),
 *  .i.e. single step execution of init/run/terminate.
 */
public class FastLambda<T>	extends Component
{
	/** Don't terminate immediately after body. */
	// Set to true for memory benchmarking
	public static boolean	KEEPALIVE	= false;
	
	/** The future to set the result on, if any. */
	protected Future<T>	result;
	
	public FastLambda(Object body, Future<T> result)
	{
		super(body);
		this.result	= result;
	}
}
