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
		IFuture<IComponentHandle> ret = Component.createComponent(new Component(body, cid, app));
		ret.then(handle -> {
			handle.scheduleStep(comp ->
			{
				try
				{
					addResultHandler(comp);
					body.run();
				}
				catch(Exception e)
				{
					// Force exception handling inside component and not in scheduleStep() return future.
					((Component)comp).handleException(e);
				}
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
		IFuture<IComponentHandle> ret = Component.createComponent(new Component(body, cid, app));
		ret.then(handle -> {
			handle.scheduleStep(comp ->
			{
				try
				{
					addResultHandler(comp);
					Object	result	= body.call();
					setResult(comp, result);
				}
				catch(Exception e)
				{
					// Force exception handling inside component and not in scheduleStep() return future.
					((Component)comp).handleException(e);
				}
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
		IFuture<IComponentHandle> ret = Component.createComponent(new Component(body, cid, app));
		ret.then(handle -> {
			handle.scheduleStep(comp ->
			{
				try
				{
					addResultHandler(comp);
					Object	result	= body.apply(comp);
					setResult(comp, result);
				}
				catch(Exception e)
				{
					// Force exception handling inside component and not in scheduleStep() return future.
					((Component)comp).handleException(e);
				}
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
		IFuture<IComponentHandle> ret = Component.createComponent(new Component(body, cid, app));
		ret.then(handle -> {
			handle.scheduleStep(comp ->
			{
				try
				{
					addResultHandler(comp);
					body.accept(comp);
				}
				catch(Exception e)
				{
					// Force exception handling inside component and not in scheduleStep() return future.
					((Component)comp).handleException(e);
				}
			});
		});
		return ret;
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
	
	//-------- result handling --------

	private static <T> void addResultHandler(IComponent comp)
	{
		Object	pojo	= comp.getPojo();
		if(pojo instanceof IResultProvider)
		{
			ExecutionFeatureProvider.addResultHandler(comp.getId(), (IResultProvider)pojo);
		}
	}
	
	private static <T> void setResult(IComponent comp, Object result)	
	{
		Object	pojo	= comp.getPojo();
		ExecutionFeatureProvider.setResult(comp.getId(), "result",
			ExecutionFeatureProvider.copyVal(result, ExecutionFeatureProvider.getAnnos(pojo.getClass())));
	}
}
