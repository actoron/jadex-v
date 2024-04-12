package jadex.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadex.common.NameValue;
import jadex.future.SubscriptionIntermediateFuture;

public class ResultProvider implements IResultProvider
{
	protected List<SubscriptionIntermediateFuture<NameValue>> resultsubscribers = new ArrayList<SubscriptionIntermediateFuture<NameValue>>();
	protected Map<String, Object> results = new HashMap<String, Object>();
	
	public Map<String, Object> getResultMap()
	{
		return results;
	}
	
	public List<SubscriptionIntermediateFuture<NameValue>> getResultSubscribers()
	{
		return resultsubscribers;
	}
}
