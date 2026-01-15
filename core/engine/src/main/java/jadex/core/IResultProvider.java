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
	/** Simple state holder for finished handling. */
	public static class	Finished {boolean finished; Exception exception;}
	
	/** Get the finished state holder. */
	public abstract Finished getFinishedState();
	
	public abstract Map<String, Object> getResultMap();
	public abstract List<SubscriptionIntermediateFuture<ChangeEvent>> getResultSubscribers();
	
	/**
	 *  Terminate all active subscriptions.
	 */
	public default void	setFinished(Exception e)
	{
		List<SubscriptionIntermediateFuture<ChangeEvent>>	subs;
		synchronized(this)
		{
			getFinishedState().finished	= true;
			getFinishedState().exception	= e;
			subs	= new ArrayList<>(getResultSubscribers());
		}
		for(SubscriptionIntermediateFuture<ChangeEvent> sub: subs)
		{
			if(e==null)
			{
				sub.setFinished();
			}
			else
			{
				sub.setException(e);
			}			
		}
	}
	
	public default void	setResult(String name, Object value)
	{
		Object	old;
		List<SubscriptionIntermediateFuture<ChangeEvent>> subs = null;
		synchronized(this)
		{
			old	= getResultMap().put(name, value);
			if(getResultSubscribers()!=null)
			{
				subs = new ArrayList<SubscriptionIntermediateFuture<ChangeEvent>>(getResultSubscribers());
			}
		}
		
		if(subs!=null)
		{
			ChangeEvent	event	= new ChangeEvent(Type.CHANGED, name, value, old, null);
			subs.forEach(sub -> 
			{
				sub.addIntermediateResult(event);
			});
		}
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
				sub.addIntermediateResult(event);
			});
		}
	}
	
	public default ISubscriptionIntermediateFuture<ChangeEvent> subscribeToResults()
	{
		SubscriptionIntermediateFuture<ChangeEvent> ret;
		Map<String, Object> res = null;
		boolean	finished;
		
		synchronized(this)
		{
			res = new HashMap<String, Object>(getResultMap());
			ret = new SubscriptionIntermediateFuture<>();
			finished	= getFinishedState().finished;
			
			if(!finished)
			{
				getResultSubscribers().add(ret);
				
				ret.setTerminationCommand(ex ->
				{
					synchronized(this)
					{
						getResultSubscribers().remove(ret);
					}
				});
			}
		}
		
		if(res!=null)
		{
			res.entrySet().stream().forEach(e -> 
			{
				if(checkInitialNotify(e.getKey(), e.getValue()))
				{
					ret.addIntermediateResult(new ChangeEvent(Type.INITIAL,	e.getKey(), e.getValue(), null, null));
				}
			});
		}
		if(finished)
		{
			if(getFinishedState().exception==null)
			{
				ret.setFinished();
			}
			else
			{
				ret.setException(getFinishedState().exception);
			}
		}
		
		return ret;
	}
	
	public default boolean checkInitialNotify(String name, Object value)
	{
		return true;
	}
}
