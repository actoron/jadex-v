package jadex.execution;

import java.util.concurrent.Callable;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.execution.impl.FastLambda;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Create minimal components, just from a lambda function.
 */
public class LambdaAgent //extends Component
{
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(Runnable body)
	{
		return create(body, null, null);
	}
	
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(IThrowingConsumer<IComponent> body)
	{
		return create(body, null, null);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(Callable<?> body)
	{
		return create(body, null, null);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(IThrowingFunction<IComponent, ?> body)
	{
		return create(body, null, null);
	}
	
	/**
	 *  Create a component with a body.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(Runnable body, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(new Component(body, cid, app));
	}
	
	/**
	 *  Create a component with a body.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(Callable<?> body, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(new Component(body, cid, app));
	}
	
	/**
	 *  Create a component with a body.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(IThrowingFunction<IComponent, ?> body, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(new Component(body, cid, app));
	}
	
	/**
	 *  Create a component with a body.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IFuture<IComponentHandle> create(IThrowingConsumer<IComponent> body, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(new Component(body, cid, app));
	}
	
	//-------- Fast Lambda methods --------
	
	public static <T>	IFuture<T> run(Callable<T> body)
	{
		return run(body, null, null);
	}
	
	public static <T>	IFuture<T> run(Callable<T> body, ComponentIdentifier cid, Application app)
	{
		Future<T>	ret	= new Future<>();
		Component.createComponent(new FastLambda<>(body, cid, app, ret));
		return ret;
	}
	
	public static <T>	IFuture<T> run(IThrowingFunction<IComponent, T> body)
	{
		return run(body, null, null);
	}
	
	public static <T>	IFuture<T> run(IThrowingFunction<IComponent, T> body, ComponentIdentifier cid, Application app)
	{
		Future<T>	ret	= new Future<>();
		Component.createComponent(new FastLambda<>(body, cid, app, ret));
		return ret;
	}
	
	public static	IFuture<Void> run(Runnable body)
	{
		return run(body, null, null);
	}
	
	public static	IFuture<Void> run(Runnable body, ComponentIdentifier cid, Application app)
	{
		Future<Void>	ret	= new Future<>();
		Component.createComponent(new FastLambda<>(body, cid, app, ret));
		return ret;
	}
	
	public static	IFuture<Void> run(IThrowingConsumer<IComponent> body)
	{
		return run(body, null, null);
	}
	
	public static	IFuture<Void> run(IThrowingConsumer<IComponent> body, ComponentIdentifier cid, Application app)
	{
		Future<Void>	ret	= new Future<>();
		Component.createComponent(new FastLambda<>(body, cid, app, ret));
		return ret;
	}
}
