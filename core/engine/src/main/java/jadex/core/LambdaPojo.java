package jadex.core;

import java.util.concurrent.Callable;

public class LambdaPojo<T> extends ResultProvider
{
	protected Object body;
	
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
		if(results.size()>0)
		{
			@SuppressWarnings("unchecked")
			T	ret	= (T)results.values().iterator().next();
			return  ret;
		}
		else
		{
			return null;
		}
	}
};