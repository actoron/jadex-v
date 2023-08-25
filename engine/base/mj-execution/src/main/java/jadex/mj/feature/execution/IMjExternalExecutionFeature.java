package jadex.mj.feature.execution;

import java.util.function.Supplier;

import jadex.future.IFuture;

/**
 *  The execution feature controls how and when components execute their steps,
 *  e.g., single-threaded vs parallel steps, real-time vs. simulation time.
 */
public interface IMjExternalExecutionFeature
{
	/**
	 *  Schedule a step to be run on the component.
	 *  @param step	A step that is executed via the {@link Runnable#run()} method.
	 */
	public void scheduleStep(Runnable step);
	
	/**
	 *  Schedule a step to be run on the component.
	 *  @param step	A step that is executed via the {@link Runnable#run()} method.
	 */
	//public void scheduleStep(int priority, Runnable step);
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public <T> IFuture<T> scheduleStep(Supplier<T> step);
	
	/**
	 *  Test if the current thread is used for current component execution.
	 *  @return True, if it is the currently executing component thread.
	 */
	public boolean isComponentThread();

	/**
	 *  Wait a specific amount.
	 *  @param millis	The time to wait (in milliseconds).
	 *  @return	A future that is finished when the time has passed.
	 */
	public IFuture<Void>	waitForDelay(long millis);
}
