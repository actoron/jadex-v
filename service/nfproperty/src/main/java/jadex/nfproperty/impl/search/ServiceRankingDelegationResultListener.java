package jadex.nfproperty.impl.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import jadex.common.Tuple2;
import jadex.future.IResultListener;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.IntermediateDelegationResultListener;
import jadex.future.TerminableIntermediateDelegationFuture;

/**
 *  Listener that ranks results.
 */
public class ServiceRankingDelegationResultListener<S> extends IntermediateDelegationResultListener<S>
{
	/** The saved results. */
	protected Collection<S> results = new LinkedHashSet<S>();
	
	/** The listener state (false=unfinished, null=finishing, true=finished. */
	protected boolean closed = false;
	protected boolean finished = false;
	protected boolean hasranked = false;
	
	/** The ranker. */
	protected IServiceRanker<S> ranker;
	
	/** The termination decider. */
	protected IRankingSearchTerminationDecider<S> decider;
	
	/**
	 *  Create a new ranker.
	 */
	public ServiceRankingDelegationResultListener(TerminableIntermediateDelegationFuture<S> future, ITerminableIntermediateFuture<S> src, 
		IServiceRanker<S> ranker, IRankingSearchTerminationDecider<S> decider)
	{
		super(future);
		this.ranker = ranker;
		this.decider = decider;
	}
	
	/**
	 *  Process intermediate results for ranking.
	 */
	protected int opencalls = 0;
	public void customIntermediateResultAvailable(S result)
	{
		if(!closed)
		{			
			opencalls++;
			results.add(result);
			if(decider!=null)
			{
				decider.isStartRanking(results, ranker instanceof IServiceEvaluator? (IServiceEvaluator) ranker : null).addResultListener(new IResultListener<Boolean>()
				{
					public void resultAvailable(Boolean fini)
					{
						opencalls--;
						if(!closed)
							closed = true;
						if(fini && opencalls==0)
							rankResults();
					}
					
					public void exceptionOccurred(Exception exception)
					{
						opencalls--;
						notifyException(exception);
					}
				});
			}
			else
			{
				results.add(result);
				opencalls--;
			}
		}
		else
		{
			System.out.println("Ignoring late result: "+result);
		}
//		customIntermediateResultAvailable(result);
	}
	
	/**
	 *  Called when result is available.
	 */
	public void customResultAvailable(Collection<S> result)
	{
		for(S res: result)
		{
			intermediateResultAvailable(res);
		}
		finished();
	}
	
	/**
	 *  Called when exception occurs.
	 */
	public void exceptionOccurred(Exception exception)
	{
		notifyException(exception);
	}
	
	/**
	 * 
	 */
	public void finished()
	{
		rankResults();
	}
	
	/**
	 *  Get the finished.
	 *  @return The finished.
	 */
	public boolean isFinished()
	{
		return finished;
	}
	
	/**
	 *  Rank the results and announce them
	 */
	protected void rankResults()
	{
		if(hasranked)
			return;
		
		hasranked = true;
		
		System.out.println("start ranking: "+this.hashCode());
		
		// Terminate the source
		((TerminableIntermediateDelegationFuture<S>)future).terminate();
			
		ranker.rankWithScores(new ArrayList<S>(results)).addResultListener(new IResultListener<List<Tuple2<S, Double>>>()
		{
			public void resultAvailable(List<Tuple2<S, Double>> result)
			{
				notifyResults(result);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				notifyException(exception);
			}
		});
	}
	
	/**
	 * 
	 */
	protected void notifyResults(List<Tuple2<S, Double>> results)
	{
		if(!isFinished())
		{
			finished = true;
			
			for(Tuple2<S, Double> res: results)
			{
				future.addIntermediateResult(res.getFirstEntity());
			}
			future.setFinished();
		}
	}
	
	/**
	 * 
	 */
	protected void notifyException(Exception exception)
	{
		if(!isFinished())
		{
			finished = true;
			future.setException(exception);
		}
	}
}

