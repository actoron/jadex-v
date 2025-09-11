package jadex.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadex.future.SubscriptionIntermediateFuture;

public class ResultProvider implements IResultProvider
{
	protected List<SubscriptionIntermediateFuture<ResultEvent>> resultsubscribers = new ArrayList<>();
	protected Map<String, Object> results = new LinkedHashMap<String, Object>();
	
	public Map<String, Object> getResultMap()
	{
		return results;
	}
	
	public List<SubscriptionIntermediateFuture<ResultEvent>> getResultSubscribers()
	{
		return resultsubscribers;
	}
}
