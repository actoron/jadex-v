package jadex.execution;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.execution.impl.ExecutionFeature;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;

/**
 *  The execution feature controls how and when components execute their steps,
 *  e.g., single-threaded vs parallel steps, real-time vs. simulation time.
 */
public interface IExecutionFeature extends IComponentFeature
{
//	/** Constant for unset step level. */
//	public static final int STEP_PRIORITY_UNSET = -1;
//	
//	/** Constant for first normal step level. */
//	public static final int STEP_PRIORITY_NORMAL = 0;
//
//	/** Constant for first immediate step level. */
//	public static final int STEP_PRIORITY_IMMEDIATE = 100;
	
	/**
	 *  Get the feature instance of the currently running component.
	 */
	public static IExecutionFeature	get()
	{
		IExecutionFeature	ret	= ExecutionFeature.LOCAL.get();
		if(ret==null)
			throw new IllegalCallerException("Not running inside any component. Check with isAnyComponentThread() before calling get().");
		return ret;
	}
	
	/**
	 *  Test if currently running inside a component.
	 */
	public static boolean	isAnyComponentThread()
	{
		return ExecutionFeature.LOCAL.get()!=null;
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
	public <T> IFuture<T> scheduleStep(Callable<T> step);
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link IThrowingConsumer#accept()} method.
	 */
	public void scheduleStep(IThrowingConsumer<IComponent> step);
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link IThrowingFunction#apply()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step);
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public <T> IFuture<T> scheduleAsyncStep(Callable<IFuture<T>> step);
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link IThrowingFunction#apply()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public <T> IFuture<T> scheduleAsyncStep(IThrowingFunction<IComponent, IFuture<T>> step);
	
	/**
	 *  Schedule a step that provides potenitially multiple results.
	 *  @param step	A step that is executed via the {@link IThrowingFunction#apply()} method.
	 *  @return	An intermediate future that provides access to the step result, once it is available.
	 */
	//public <T> IIntermediateFuture<T> scheduleSteps(IThrowingFunction<IComponent, IIntermediateFuture<T>> step);
	
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
	public ITerminableFuture<Void> waitForDelay(long millis);
	
	/**
	 *  Get the component to which this feature belongs. 
	 */
	public IComponent getComponent();
	
	/**
	 *  Get the current time.
	 *  @return	The time in milliseconds.
	 */
	public long	getTime();
	
	// This does not terminate the component properly. Must call component terminate before feature
	// due to ComponentManager.get().removeComponent(this.getId());
	// Or do we want to move that remove to the feature?
	/**
	 *  Terminate the component and abort all blocked steps.
	 * /
	public void terminate();*/
}
