package jadex.core;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.common.NameValue;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;

/**
 *  Interface for component access from non-component thread, e.g. UI thread.
 */
public interface IComponentHandle 
{
	/**
	 *  Get the id.
	 *  @return The id.
	 */
	public ComponentIdentifier getId();
	
	/**
	 *  Get the app id.
	 *  return The app id.
	 */
	public String getAppId();
	
	/**
	 *  Terminate the component.
	 */
	public default IFuture<Void> terminate()
	{
		return IComponentManager.get().terminate(getId());
	}
	
	/**
	 *  Wait for termination.
	 *  @return True on termination; false on component not found.
	 */
	public default IFuture<Boolean> waitForTermination()
	{
		return IComponentManager.get().waitForTermination(getId());
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
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public default <T> IFuture<T> scheduleAsyncStep(Callable<IFuture<T>> step)
	{
		throw new UnsupportedOperationException("Missing execution feature");
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link IThrowingFunction#apply()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public default <T> IFuture<T> scheduleAsyncStep(IThrowingFunction<IComponent, IFuture<T>> step)
	{
		throw new UnsupportedOperationException("Missing execution feature");
	}
	
	/**
	 *  Get the local pojo. Allows for calling pojo methods.
	 *  @return The pojo.
	 */
	public default <T> T getPojoHandle(Class<T> type)
	{
		throw new UnsupportedOperationException("Missing execution feature");
	}
	
	/**
	 *  Fetch the result(s) of the Component.
	 */
	public IFuture<Map<String, Object>> getResults();
	
	/**
	 *  Listen to results of the component.
	 *  @throws UnsupportedOperationException when subscription is not supported
	 */
	public ISubscriptionIntermediateFuture<NameValue> subscribeToResults();
}
