package jadex.execution;

import java.lang.annotation.Annotation;
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
	public record Result<T>(IComponentHandle component, IFuture<T> result){};
	
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IComponentHandle create(Runnable body)
	{
		return create(body, null, null);
	}
	
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IComponentHandle create(IThrowingConsumer<IComponent> body)
	{
		return create(body, null, null);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> Result<T> create(Callable<T> body)
	{
		return create(body, null, null);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> Result<T> create(IThrowingFunction<IComponent, T> body)
	{
		return create(body, null, null);
	}
	
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IComponentHandle create(Runnable body, ComponentIdentifier cid, Application app)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		addResultHandle(comp, null);
		comp.getComponentHandle().scheduleStep(() -> body);
		return comp.getComponentHandle();
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> Result<T> create(Callable<T> body, ComponentIdentifier cid, Application app)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		IFuture<T> res = comp.getComponentHandle().scheduleStep(body);
		addResultHandle(comp, res, ExecutionFeatureProvider.getAnnos(body.getClass()));
		return new Result<T>(comp.getComponentHandle(), res);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> Result<T> create(IThrowingFunction<IComponent, T> body, ComponentIdentifier cid, Application app)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		IFuture<T> res = comp.getComponentHandle().scheduleStep(body);
		addResultHandle(comp, res, ExecutionFeatureProvider.getAnnos(body.getClass()));
		return new Result<T>(comp.getComponentHandle(), res);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IComponentHandle create(IThrowingConsumer<IComponent> body, ComponentIdentifier cid, Application app)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		addResultHandle(comp, null);
		comp.getComponentHandle().scheduleStep(body);
		return comp.getComponentHandle();
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

	private static <T> void addResultHandle(Component comp, IFuture<T> result, Annotation... annos)
	{
		Object	pojo	= comp.getPojo();
		if(pojo instanceof IResultProvider)
		{
			if(result!=null)
			{
				throw new UnsupportedOperationException("Implementing IResultProvider not supported on callable/function: "+pojo);
			}
			ExecutionFeatureProvider.addResultHandler(comp.getId(), (IResultProvider)pojo);
		}
		else if(result!=null)
		{
			// Copy result on add
			result.then(res -> ExecutionFeatureProvider.addResult(comp.getId(), "result", ExecutionFeatureProvider.copyVal(res, annos)));
		}
	}
}
