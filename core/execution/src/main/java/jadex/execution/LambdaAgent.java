package jadex.execution;

import java.util.concurrent.Callable;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.LambdaPojo;
import jadex.core.impl.Component;
import jadex.execution.impl.FastLambda;
import jadex.future.IFuture;

/**
 *  Create minimal components, just from a lambda function.
 */
public class LambdaAgent //extends Component
{
	public record Result<T>(IExternalAccess component, IFuture<T> result){};
	
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
	public static IExternalAccess create(Runnable body)
	{
		return create(body, null);
	}
	
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IExternalAccess create(IThrowingConsumer<IComponent> body)
	{
		return create(body, null);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> Result<T> create(Callable<T> body)
	{
		return create(body, null);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> Result<T> create(IThrowingFunction<IComponent, T> body)
	{
		return create(body, null);
	}
	
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static IExternalAccess create(Runnable body, ComponentIdentifier cid)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(cid));
		comp.getExternalAccess().scheduleStep(() -> body);
		/*{
			body.run();
			comp.terminate();
		});*/
		return comp.getExternalAccess();
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	//public static <T> IFuture<T> create(Callable<T> body, ComponentIdentifier cid)
	public static <T> Result<T> create(Callable<T> body, ComponentIdentifier cid)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(cid));
		IFuture<T> res = comp.getExternalAccess().scheduleStep(body);
		//res.then(r -> comp.terminate()).catchEx(ex -> comp.terminate());
		//comp.result = res;
		return new Result<T>(comp.getExternalAccess(), res);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> Result<T> create(IThrowingFunction<IComponent, T> body, ComponentIdentifier cid)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(cid));
		IFuture<T> res = comp.getExternalAccess().scheduleStep(body);
		//res.then(r -> comp.terminate()).catchEx(ex -> comp.terminate());
		//comp.result = res;
		return new Result<T>(comp.getExternalAccess(), res);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IExternalAccess create(IThrowingConsumer<IComponent> body, ComponentIdentifier cid)
	{
		Component comp = Component.createComponent(Component.class, () -> new Component(cid));
		comp.getExternalAccess().scheduleStep(body);
		return comp.getExternalAccess();
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IExternalAccess create(LambdaPojo<T> pojo, ComponentIdentifier cid)
	{
		IExternalAccess ret = null;
		Object body = pojo.getBody();
		if(body instanceof IThrowingFunction)
		{
			Result<T> res = create((IThrowingFunction)body, cid);
			ret = res.component();
			res.result().then(r -> 
				pojo.addResult("result", r));
		}
		else if(body instanceof Callable)
		{
			Result<T> res = create((Callable)body, cid);
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
	
	public static <T>	T run(IThrowingFunction<IComponent, T> body)
	{
		@SuppressWarnings("unchecked")
		FastLambda<T> comp = Component.createComponent(FastLambda.class, () -> new FastLambda<>(body, true));
		return comp.getResult();
	}
}
