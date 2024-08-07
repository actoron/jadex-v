package jadex.nfproperty.impl.search;

import jadex.future.IFuture;
import jadex.providedservice.IService;

public interface IServiceEvaluator
{
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
	public IFuture<Double> evaluate(IService service);
}
