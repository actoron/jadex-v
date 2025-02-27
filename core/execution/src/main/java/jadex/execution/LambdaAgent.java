package jadex.execution;

import java.util.concurrent.Callable;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.LambdaPojo;
import jadex.core.impl.Component;
import jadex.execution.impl.FastLambda;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Create minimal components, just from a lambda function.
 */
public class LambdaAgent //extends Component
{
	public record Result<T>(IComponentHandle component, IFuture<T> result){};
	
	/*public static class LambdaPojo<T> 
	{
		protected Object body;
		protected IFuture<?> result;
		
		public LambdaPojo(Callable<T> body, IFuture<?> result)
		{
			this.body = body;
			this.result = result;
		}
		
		public LambdaPojo(IThrowingFunction<T, ?> body, IFuture<?> result)
		{
			this.body = body;
			this.result = result;
		}

		public Object getBody() 
		{
			return body;
		}

		public IFuture<?> getResult() 
		{
			return result;
		}
	};*/
	
	//public IFuture<?> result;
	
	/*public LambdaAgent()
	{
		super(null);
	}*/
	
	/*public LambdaAgent(ComponentIdentifier id)
	{
		super(id);
	}*/
	
	/*public Map<String, Object> getResults(Object pojo)
	{
		Map<String, Object> ret = new HashMap<String, Object>();
		if(result!=null && result.isDone())
		{
			ret.put("result", result.get());
		}
		return ret;
	}*/
	
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
		comp.getComponentHandle().scheduleStep(() -> body);
		/*{
			body.run();
			comp.terminate();
		});*/
		return comp.getComponentHandle();
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	//public static <T> IFuture<T> create(Callable<T> body, ComponentIdentifier cid)
	public static <T> Result<T> create(Callable<T> body, ComponentIdentifier cid, Application app)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		IFuture<T> res = comp.getComponentHandle().scheduleStep(body);
		//res.then(r -> comp.terminate()).catchEx(ex -> comp.terminate());
		//comp.result = res;
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
		//res.then(r -> comp.terminate()).catchEx(ex -> comp.terminate());
		//comp.result = res;
		return new Result<T>(comp.getComponentHandle(), res);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IComponentHandle create(IThrowingConsumer<IComponent> body, ComponentIdentifier cid, Application app)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(body, cid, app));
		comp.getComponentHandle().scheduleStep(body);
		return comp.getComponentHandle();
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IComponentHandle create(LambdaPojo<T> pojo, ComponentIdentifier cid, Application app)
	{
		IComponentHandle ret = null;
		Object body = pojo.getBody();
		if(body instanceof IThrowingFunction)
		{
			Result<T> res = create((IThrowingFunction)body, cid, app);
			ret = res.component();
			res.result().then(r -> 
				pojo.addResult("result", r));
		}
		else if(body instanceof Callable)
		{
			Result<T> res = create((Callable)body, cid, app);
			ret = res.component();
			res.result().then(r -> 
				pojo.addResult("result", r));
		}
		/*else if(body instanceof IThrowingConsumer)
		{
			create((IThrowingConsumer)body, cid);
		}
		else if(body instanceof Runnable)
		{
			create((Runnable)body, cid);
		}*/
		else
		{
			throw new RuntimeException("Body type unknown: "+body);
		}
		
		return ret;
	}
	
	//-------- Fast Lambda methods --------
	
	public static <T>	IFuture<T> run(IThrowingFunction<IComponent, T> body)
	{
		Future<T>	ret	= new Future<>();
		Component.createComponent(FastLambda.class, () -> new FastLambda<>(body, ret, true));
		return ret;
	}
}
