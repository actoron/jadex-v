package jadex.mj.feature.execution;

import java.util.concurrent.Callable;

import jadex.future.IFuture;
import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.core.IThrowingConsumer;
import jadex.mj.core.IThrowingFunction;
import jadex.mj.core.impl.MjComponent;

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
		MjComponent	comp	= MjComponent.createComponent(MjComponent.class, () -> new MjComponent(cid));
		IMjExecutionFeature.getExternal(comp).scheduleStep(body);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IFuture<T>	create(Callable<T> body, ComponentIdentifier cid)
	{
		MjComponent	comp	= MjComponent.createComponent(MjComponent.class, () -> new MjComponent(cid));
		return IMjExecutionFeature.getExternal(comp).scheduleStep(body);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IFuture<T>	create(IThrowingFunction<IComponent, T> body, ComponentIdentifier cid)
	{
		MjComponent	comp = MjComponent.createComponent(MjComponent.class, () -> new MjComponent(cid));
		return IMjExecutionFeature.getExternal(comp).scheduleStep(body);
	}

	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> void	create(IThrowingConsumer<IComponent> body, ComponentIdentifier cid)
	{
		MjComponent	comp = MjComponent.createComponent(MjComponent.class, () -> new MjComponent(cid));
		IMjExecutionFeature.getExternal(comp).scheduleStep(body);
	}
}
