package jadex.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadex.common.NameValue;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;

public interface IResultProvider 
{
	public abstract Map<String, Object> getResultMap();
	public abstract List<SubscriptionIntermediateFuture<NameValue>> getResultSubscribers();
	
	public default void	addResult(String name, Object value)
	{
		synchronized(this)
		{
			getResultMap().put(name, value);
		}
		notifyResult(name, value);
	}
	
	public default void notifyResult(String name, Object value)
	{
		List<SubscriptionIntermediateFuture<NameValue>> subs = null;
		synchronized(this)
		{
			if(getResultSubscribers()!=null)
				subs = new ArrayList<SubscriptionIntermediateFuture<NameValue>>(getResultSubscribers());
		}
		if(subs!=null)
			subs.forEach(sub -> 
			{
				//System.out.println("sub: "+name+" "+value);
				sub.addIntermediateResult(new NameValue(name, value));
			});
	}
	
	public default ISubscriptionIntermediateFuture<NameValue> subscribeToResults()
	{
		SubscriptionIntermediateFuture<NameValue> ret;
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
			res.entrySet().stream().forEach(e -> ret.addIntermediateResult(new NameValue(e.getKey(), e.getValue())));
		
		return ret;
	}
}
