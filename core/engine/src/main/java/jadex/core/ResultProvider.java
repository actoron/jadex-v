package jadex.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadex.future.SubscriptionIntermediateFuture;

public class ResultProvider implements IResultProvider
{
	protected List<SubscriptionIntermediateFuture<ChangeEvent>> resultsubscribers = new ArrayList<>(1);
	protected Map<String, Object> results = new LinkedHashMap<String, Object>(2);
	
	public Map<String, Object> getResultMap()
	{
		return results;
	}
	
	public List<SubscriptionIntermediateFuture<ChangeEvent>> getResultSubscribers()
	{
		return resultsubscribers;
	}
}
