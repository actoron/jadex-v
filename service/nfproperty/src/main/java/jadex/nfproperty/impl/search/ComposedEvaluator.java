package jadex.nfproperty.impl.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.Tuple2;
import jadex.future.CollectionResultListener;
import jadex.future.CounterResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.providedservice.IService;

/**
 *  Evaluator composed of multiple weighted evaluators.
 */
public class ComposedEvaluator<S> implements IServiceEvaluator, IServiceRanker<S>
{
	/** The evaluators. */
	protected List<Tuple2<IServiceEvaluator, Double>> evaluators;
	
	/**
	 *  Creates the combiner.
	 */
	public ComposedEvaluator()
	{
		evaluators = new ArrayList<Tuple2<IServiceEvaluator,Double>>();
	}
	
	/**
	 *  Adds a new evaluator with a weight of 1.0.
	 *  
	 *  @param evaluator The new evaluator.
	 */
	public void addEvaluator(IServiceEvaluator evaluator)
	{
		addEvaluator(evaluator, 1.0);
	}
	
	/**
	 *  Adds a new evaluator.
	 *  
	 *  @param evaluator The new evaluator.
	 *  @param weight The weight of the evaluator relative to other evaluators.
	 */
	public void addEvaluator(IServiceEvaluator evaluator, double weight)
	{
		evaluators.add(new Tuple2<IServiceEvaluator, Double>(evaluator, weight));
	}
	
	/**
	 *  Removes an evaluator.
	 *  
	 *  @param evaluator The evaluator.
	 */
	public void removeEvaluator(IServiceEvaluator evaluator)
	{
		for (int i = 0; i < evaluators.size(); ++i)
		{
			if (evaluators.get(i).getFirstEntity().equals(evaluator))
			{
				evaluators.remove(i);
				break;
			}
		}
	}
	
	/**
	 *  Evaluates the service in detail. This method
	 *  must return an evaluation of the service in
	 *  the range between 0 (worst/unacceptable) to
	 *  1 (best/preferred).
	 * 
	 *  @param service The service being evaluated.
	 * 
	 *  @return An evaluation of the service in a
	 *  		 range between 0 and 1 (inclusive).
	 */
	public IFuture<Double> evaluate(IService service)
	{
		final Future<Double> ret = new Future<Double>();
//		double ret = 0;
		double tw = 0;
		
		for (Tuple2<IServiceEvaluator, Double> eval : evaluators)
		{
			tw += eval.getSecondEntity();
		}
		final double totalweight = tw;
		
		final CollectionResultListener<Double> crl = new CollectionResultListener<Double>(evaluators.size(), false, new IResultListener<Collection<Double>>()
		{
			public void resultAvailable(Collection<Double> result)
			{
				double res = 0;
				for (Double sres : result)
				{
					res += sres;
				}
				res /= totalweight;
				ret.setResult(res);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
		});
			
		for(Tuple2<IServiceEvaluator, Double> eval : evaluators)
		{
			final double weight = eval.getSecondEntity();
			eval.getFirstEntity().evaluate(service).addResultListener(new IResultListener<Double>()
			{
				public void resultAvailable(Double result)
				{
					crl.resultAvailable(result * weight);
				}
				
				public void exceptionOccurred(Exception exception)
				{
					crl.exceptionOccurred(exception);
				}
			});
		}
		
		return ret;
	}
	
	/**
	 *  Ranks services according to non-functional criteria.
	 *  
	 *  @param unrankedservices Unranked list of services.
	 *  @return Ranked list of services.
	 */
	public IFuture<List<S>> rank(final List<S> unrankedservices)
	{
		final Future<List<S>> ret = new Future<List<S>>();
		final Map<IService, Double> evalmap = Collections.synchronizedMap(new HashMap<IService, Double>());
		
		final CounterResultListener<Void> crl = new CounterResultListener<Void>(unrankedservices.size(), new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
				Collections.sort(unrankedservices, new Comparator<S>()
				{
					public int compare(S s1, S s2)
					{
						return (int)-Math.signum(evalmap.get(s1) - evalmap.get(s2));
					}
				});
				ret.setResult(unrankedservices);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
		});
		
		for(int i = 0; i < unrankedservices.size(); ++i)
		{
			final IService service = (IService) unrankedservices.get(i);
			
			evaluate(service).addResultListener(new IResultListener<Double>()
			{
				public void resultAvailable(Double result)
				{
					evalmap.put(service, result);
					crl.resultAvailable(null);
				}
				
				public void exceptionOccurred(Exception exception)
				{
					crl.exceptionOccurred(exception);
				}
			});
		}
		
		return ret;
	}
	
	/**
	 *  Ranks services according to non-functional criteria.
	 *  
	 *  @param unrankedservices Unranked list of services.
	 *  @return Ranked list of services and scores.
	 */
	protected static int counter = 0;
	public IFuture<List<Tuple2<S, Double>>> rankWithScores(final List<S> unrankedservices)
	{
		final int cnt = counter++;
		final Future<List<Tuple2<S, Double>>> ret = new Future<List<Tuple2<S, Double>>>();
		final Map<IService, Double> evalmap = Collections.synchronizedMap(new HashMap<IService, Double>());
		
		//System.out.println("created: "+unrankedservices.size()+" "+cnt);
		final CounterResultListener<Void> crl = new CounterResultListener<Void>(unrankedservices.size(), new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
				//System.out.println("sizes: "+unrankedservices.size()+" "+evalmap.size()+" "+cnt);
				Collections.sort(unrankedservices, new Comparator<S>()
				{
					public int compare(S s1, S s2)
					{
						Double v1 = evalmap.get(s1);
						Double v2 = evalmap.get(s2);
						//if(v1==null || v2==null)
						//	System.out.println("nulllll"+" "+cnt);
						return (int)-Math.signum(v1 - v2);
					}
				});
				List<Tuple2<S, Double>> evas = new ArrayList<Tuple2<S, Double>>();
				for(S ser: unrankedservices)
				{
					evas.add(new Tuple2<S, Double>(ser, evalmap.get(ser)));
				}
				ret.setResult(evas);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
		});
		
		Set<IService> done = new HashSet<>();
		for(int i = 0; i < unrankedservices.size(); ++i)
		{
			final IService service = (IService)unrankedservices.get(i);
			if(done.contains(service))
				System.out.println("problem: "+service+" "+done+" "+cnt);
			done.add(service);
			//System.out.println("now at: "+i+" "+service+" "+cnt);
			
			evaluate(service).addResultListener(new IResultListener<Double>()
			{
				public void resultAvailable(Double result)
				{
					//System.out.println("eval: "+result+" "+service+" "+cnt);
					//if(evalmap.containsKey(service))
					//	System.out.println("buuuuug: "+service+" "+evalmap+" "+cnt);
					evalmap.put(service, result);
					crl.resultAvailable(null);
				}
				
				public void exceptionOccurred(Exception exception)
				{
					//System.out.println("exeception is: "+exception+" "+cnt);
					crl.exceptionOccurred(exception);
				}
			});
		}
		
		return ret;
	}
}
