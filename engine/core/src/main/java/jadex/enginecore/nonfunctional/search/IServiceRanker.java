package jadex.enginecore.nonfunctional.search;

import java.util.List;

import jadex.common.Tuple2;
import jadex.future.IFuture;

/**
 *  Interfaces for non-functional ranking mechanism for services.
 */
public interface IServiceRanker<S>
{
	/**
	 *  Ranks services according to non-functional criteria.
	 *  
	 *  @param unrankedservices Unranked list of services.
	 *  @return Ranked list of services.
	 */
	public IFuture<List<S>> rank(List<S> unrankedservices);
	
	/**
	 *  Ranks services according to non-functional criteria.
	 *  
	 *  @param unrankedservices Unranked list of services.
	 *  @return Ranked list of services and scores.
	 */
	public IFuture<List<Tuple2<S, Double>>> rankWithScores(List<S> unrankedservices);
}
