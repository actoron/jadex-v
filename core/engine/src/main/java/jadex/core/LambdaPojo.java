package jadex.core;

import java.util.concurrent.Callable;

public class LambdaPojo<T> 
{
	protected Object body;
	protected T result;
	
	public LambdaPojo(Callable<T> body)
	{
		this.body = body;
	}
	
	public LambdaPojo(IThrowingFunction<IComponent, T> body)
	{
		this.body = body;
	}

	public Object getBody() 
	{
		return body;
	}

	public T getResult() 
	{
		return result;
	}

	public void setResult(T result) 
	{
		this.result = result;
	}
};