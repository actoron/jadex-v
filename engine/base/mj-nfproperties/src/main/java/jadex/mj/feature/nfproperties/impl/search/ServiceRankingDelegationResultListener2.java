package jadex.mj.feature.nfproperties.impl.search;

import java.util.List;

import jadex.common.Tuple2;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.TerminableIntermediateDelegationFuture;

/**
 *  Listener that ranks results.
 */
//public class ServiceRankingDelegationResultListener2<S> extends ServiceRankingDelegationResultListener<Tuple2<S, Double>>
public class ServiceRankingDelegationResultListener2<S> extends ServiceRankingDelegationResultListener<S>
{
	/**
	 *  Create a new ranker.
	 */
	public ServiceRankingDelegationResultListener2(TerminableIntermediateDelegationFuture<Tuple2<S, Double>> future, ITerminableIntermediateFuture<S> src, 
		IServiceRanker<S> ranker, IRankingSearchTerminationDecider<S> decider)
	{
		super((TerminableIntermediateDelegationFuture)future, src, ranker, decider);
	}
	
	/**
	 *  Notify the results,
	 */
	protected void notifyResults(List<Tuple2<S, Double>> results)
	{
		if(!isFinished())
		{
			finished = Boolean.TRUE;
			
			TerminableIntermediateDelegationFuture<Tuple2<S, Double>> fut = (TerminableIntermediateDelegationFuture<Tuple2<S, Double>>)future;
			for(Tuple2<S, Double> res: results)
			{
				fut.addIntermediateResult(res);
			}
			fut.setFinished();
		}
	}
}

