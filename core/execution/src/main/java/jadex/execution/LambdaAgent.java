package jadex.execution;

import java.util.concurrent.Callable;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IResultProvider;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.execution.impl.ExecutionFeatureProvider;
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
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(Runnable body, ComponentIdentifier cid, Application app)
	{
		IFuture<IComponentHandle> ret = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		ret.then(handle -> {
			handle.scheduleStep(comp ->
			{
				addResultHandler(comp);
				body.run();
			});
		});
		return ret;
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(Callable<?> body, ComponentIdentifier cid, Application app)
	{
		IFuture<IComponentHandle> ret = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		ret.then(handle -> {
			handle.scheduleStep(comp ->
			{
				addResultHandler(comp);
				Object	result	= body.call();
				addResult(comp, result);
			});
		});
		return ret;
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IFuture<IComponentHandle>	create(IThrowingFunction<IComponent, ?> body, ComponentIdentifier cid, Application app)
	{
		IFuture<IComponentHandle> ret = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		ret.then(handle -> {
			handle.scheduleStep(comp ->
			{
				addResultHandler(comp);
				Object	result	= body.apply(comp);
				addResult(comp, result);
			});
		});
		return ret;
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IFuture<IComponentHandle> create(IThrowingConsumer<IComponent> body, ComponentIdentifier cid, Application app)
	{
		IFuture<IComponentHandle> ret = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		ret.then(handle -> {
			handle.scheduleStep(comp ->
			{
				addResultHandler(comp);
				body.accept(comp);
			});
		});
		return ret;
	}
	
	//-------- Fast Lambda methods --------
	
	public static <T>	IFuture<T> run(Callable<T> body)
	{
		Future<T>	ret	= new Future<>();
		Component.createComponent(FastLambda.class, () -> new FastLambda<>(body, ret, true));
		return ret;
	}
	
	public static <T>	IFuture<T> run(IThrowingFunction<IComponent, T> body)
	{
		Future<T>	ret	= new Future<>();
		Component.createComponent(FastLambda.class, () -> new FastLambda<>(body, ret, true));
		return ret;
	}
	
	public static	IFuture<Void> run(Runnable body)
	{
		Future<Void>	ret	= new Future<>();
		Component.createComponent(FastLambda.class, () -> new FastLambda<>(body, ret, true));
		return ret;
	}
	
	public static	IFuture<Void> run(IThrowingConsumer<IComponent> body)
	{
		Future<Void>	ret	= new Future<>();
		Component.createComponent(FastLambda.class, () -> new FastLambda<>(body, ret, true));
		return ret;
	}
	
	//-------- result handling --------

	private static <T> void addResultHandler(IComponent comp)
	{
		Object	pojo	= comp.getPojo();
		if(pojo instanceof IResultProvider)
		{
			ExecutionFeatureProvider.addResultHandler(comp.getId(), (IResultProvider)pojo);
		}
	}
	
	private static <T> void addResult(IComponent comp, Object result)	
	{
		Object	pojo	= comp.getPojo();
		ExecutionFeatureProvider.addResult(comp.getId(), "result",
			ExecutionFeatureProvider.copyVal(result, ExecutionFeatureProvider.getAnnos(pojo.getClass())));
	}
}
