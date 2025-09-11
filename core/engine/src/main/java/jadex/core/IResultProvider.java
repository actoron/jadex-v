package jadex.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadex.core.ResultEvent.Type;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;

public interface IResultProvider 
{
	public abstract Map<String, Object> getResultMap();
	public abstract List<SubscriptionIntermediateFuture<ResultEvent>> getResultSubscribers();
	
	public default void	setResult(String name, Object value)
	{
		synchronized(this)
		{
			getResultMap().put(name, value);
		}
		notifyResult(new ResultEvent(name, value));
	}
	
	public default void notifyResult(ResultEvent event)
	{
		List<SubscriptionIntermediateFuture<ResultEvent>> subs = null;
		synchronized(this)
		{
			if(getResultSubscribers()!=null)
				subs = new ArrayList<SubscriptionIntermediateFuture<ResultEvent>>(getResultSubscribers());
		}
		if(subs!=null)
		{
			subs.forEach(sub -> 
			{
				//System.out.println("sub: "+name+" "+value);
				//if(checkNotify(name, value))
				sub.addIntermediateResult(event);
			});
		}
	}
	
	public default ISubscriptionIntermediateFuture<ResultEvent> subscribeToResults()
	{
		SubscriptionIntermediateFuture<ResultEvent> ret;
		Map<String, Object> res = null;
		
		synchronized(this)
		{
			res = new HashMap<String, Object>(getResultMap());
			ret = new SubscriptionIntermediateFuture<>();
			
			getResultSubscribers().add(ret);
			
			ret.setTerminationCommand(ex ->
			{
				synchronized(this)
				{
					getResultSubscribers().remove(ret);
				}
			});
		}
		
		if(res!=null)
		{
			res.entrySet().stream().forEach(e -> 
			{
				if(checkInitialNotify(e.getKey(), e.getValue()))
					ret.addIntermediateResult(new ResultEvent(Type.INITIAL,	e.getKey(), e.getValue(), null, null));
			});
		}
		
		return ret;
	}
	
	public default boolean checkInitialNotify(String name, Object value)
	{
		return true;
	}
}
