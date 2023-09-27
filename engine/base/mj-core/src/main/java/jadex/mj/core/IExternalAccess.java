package jadex.mj.core;

import java.util.UUID;
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
	public UUID getId();
	
	/**
	 *  Schedule a step to be run on the component.
	 *  @param step	A step that is executed via the {@link Runnable#run()} method.
	 */
	public void scheduleStep(Runnable step);
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public <T> IFuture<T> scheduleStep(Supplier<T> step);
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link IThrowingConsumer#accept()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public void scheduleStep(IThrowingConsumer<IComponent> step);
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link IThrowingFunction#apply()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step);
}
