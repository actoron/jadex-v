package jadex.core;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.future.IFuture;

/**
 *  Interface for component access from non-component thread, e.g. UI thread.
 */
public interface IExternalAccess 
{
	/**
	 *  Get the id.
	 *  @return The id.
	 */
	public ComponentIdentifier getId();
	
	/**
	 *  Check if this component allows the execution of steps.
	 *  Otherwise scheduleStep(...) methods with throw UnsupportedOperationException
	 */
	public default boolean	isExecutable()
	{
		return false;
	}
	
	/**
	 *  Terminate the component.
	 */
	public default IFuture<Void> terminate()
	{
		return IComponent.terminate(getId());
	}
	
	/**
	 *  Schedule a step to be run on the component.
	 *  @param step	A step that is executed via the {@link Runnable#run()} method.
	 */
	public default void scheduleStep(Runnable step)
	{
		throw new UnsupportedOperationException("Missing execution feature");
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public default <T> IFuture<T> scheduleStep(Callable<T> step)
	{
		throw new UnsupportedOperationException("Missing execution feature");
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link IThrowingConsumer#accept()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public default void scheduleStep(IThrowingConsumer<IComponent> step)
	{
		throw new UnsupportedOperationException("Missing execution feature");
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link IThrowingFunction#apply()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public default <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
	{
		throw new UnsupportedOperationException("Missing execution feature");
	}
}
