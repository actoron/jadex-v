package jadex.execution;

import java.util.concurrent.Callable;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.future.IFuture;

/**
 *  Create minimal components, just from a lambda function.
 */
public class LambdaAgent
{
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static void	create(Runnable body)
	{
		create(body, null);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IFuture<T>	create(Callable<T> body)
	{
		return create(body, null);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IFuture<T>	create(IThrowingFunction<IComponent, T> body)
	{
		return create(body, null);
	}
	
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static void	create(Runnable body, ComponentIdentifier cid)
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(cid));
		comp.getExternalAccess().scheduleStep(body);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IFuture<T>	create(Callable<T> body, ComponentIdentifier cid)
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(cid));
		return comp.getExternalAccess().scheduleStep(body);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IFuture<T>	create(IThrowingFunction<IComponent, T> body, ComponentIdentifier cid)
	{
		Component	comp = Component.createComponent(Component.class, () -> new Component(cid));
		return comp.getExternalAccess().scheduleStep(body);
	}

	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> void	create(IThrowingConsumer<IComponent> body, ComponentIdentifier cid)
	{
		Component	comp = Component.createComponent(Component.class, () -> new Component(cid));
		comp.getExternalAccess().scheduleStep(body);
	}
}
