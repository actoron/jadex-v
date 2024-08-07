package jadex.nfproperty.impl.search;

import java.util.Collection;

import jadex.future.IFuture;

/**
 *  Service search ranking decider based on a simple service count threshold.
 */
public class CountThresholdSearchTerminationDecider<S> implements IRankingSearchTerminationDecider<S>
{
	/** The threshold of found services after which the ranking starts. */
	protected int threshold;
	
	/**
	 *  Creates the decider.
	 *  @param threshold The threshold of found services after which the ranking starts.
	 */
	public CountThresholdSearchTerminationDecider(int threshold)
	{
		this.threshold = threshold;
	}
	
	/**
	 *  Decides if the search should start ranking.
	 */
	public IFuture<Boolean> isStartRanking(Collection<S> currentresults, IServiceEvaluator evaluator)
	{
		IFuture<Boolean> ret = currentresults.size()>=threshold ? IFuture.TRUE : IFuture.FALSE;
		
		//System.out.println("isStartRank: "+currentresults.size()+" "+ret.get());

		return ret;
	}
}
