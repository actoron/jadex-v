package jadex.mj.feature.execution;

import java.util.function.Supplier;

import jadex.future.IFuture;
import jadex.mj.core.MjComponent;

/**
 *  The execution feature controls how and when components execute their steps,
 *  e.g., single-threaded vs parallel steps, real-time vs. simulation time.
 */
public interface IMjExecutionFeature
{
	/** Constant for unset step level. */
	public static final int STEP_PRIORITY_UNSET = -1;
	
	/** Constant for first normal step level. */
	public static final int STEP_PRIORITY_NORMAL = 0;

	/** Constant for first immediate step level. */
	public static final int STEP_PRIORITY_IMMEDIATE = 100;
	
	/** The currently executing component (if any). */
	// Provided for fast caller/callee context-switching ?
	public static final ThreadLocal<MjComponent> LOCAL = new ThreadLocal<MjComponent>();
	
	/**
	 *  Get the feature instance of the given component.
	 */
	public static IMjExecutionFeature	of(MjComponent self)
	{
		return self.getFeature(IMjExecutionFeature.class);
	}
	
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
	
	/**
	 *  Get the current time.
	 *  @return	The time in milliseconds.
	 */
	public long	getTime();
}
