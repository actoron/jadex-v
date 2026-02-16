package jadex.execution.impl;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.Component;
import jadex.future.IFuture;

/**
 *  Marker class for Lambda Agent optimizations when started with run(),
 *  .i.e. single step execution of init/run/terminate.
 */
public class FastLambda<T>	extends Component
{
	/** Don't terminate immediately after body. */
	// Set to true for memory benchmarking
	public static boolean	KEEPALIVE	= false;
	
	/** The future result of the lambda step. */
	private IFuture<T>	result;

	/** 	 *  Create a new fast lambda agent. 	 */
	public FastLambda(Object body, ComponentIdentifier cid, Application app)
	{
		super(body, cid, app);
	}

	/**
	 *  Get the future result of the lambda step.	 */
	public IFuture<T> getResult()
	{
		return result;
	}
	
	/**
	 *  Set the future result of the lambda step.
	 */
	public void setResult(IFuture<T> result)
	{
		this.result	= result;
	}
}
