package jadex.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadex.core.ChangeEvent.Type;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;

public interface IResultProvider 
{
	public abstract Map<String, Object> getResultMap();
	public abstract List<SubscriptionIntermediateFuture<ChangeEvent>> getResultSubscribers();
	
	public default void	setResult(String name, Object value)
	{
		Object	old;
		synchronized(this)
		{
			old	= getResultMap().put(name, value);
		}
		notifyResult(new ChangeEvent(Type.CHANGED, name, value, old, null));
	}
	
	public default void notifyResult(ChangeEvent event)
	{
		List<SubscriptionIntermediateFuture<ChangeEvent>> subs = null;
		synchronized(this)
		{
			if(getResultSubscribers()!=null)
				subs = new ArrayList<SubscriptionIntermediateFuture<ChangeEvent>>(getResultSubscribers());
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
	
	public default ISubscriptionIntermediateFuture<ChangeEvent> subscribeToResults()
	{
		SubscriptionIntermediateFuture<ChangeEvent> ret;
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
					ret.addIntermediateResult(new ChangeEvent(Type.INITIAL,	e.getKey(), e.getValue(), null, null));
			});
		}
		
		return ret;
	}
	
	public default boolean checkInitialNotify(String name, Object value)
	{
		return true;
	}
}
