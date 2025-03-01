package jadex.core;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.core.impl.ComponentManager;
import jadex.future.IFuture;

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
	 *  Get the external access.
	 *  @param The id of the component.
	 *  @return The external access.
	 */
	public default IComponentHandle getExternalAccess(ComponentIdentifier cid)
	{
		return ComponentManager.get().getComponent(cid).getComponentHandle();
	}
	
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
	public <T> T getPojoHandle(Class<T> type);
	
}
