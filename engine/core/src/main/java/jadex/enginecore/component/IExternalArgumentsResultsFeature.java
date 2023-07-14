package jadex.enginecore.component;

import java.util.Map;

import jadex.common.Tuple2;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;

/**
 * 
 */
public interface IExternalArgumentsResultsFeature extends IExternalComponentFeature
{
	/**
	 *  Get the component results.
	 *  @return The results.
	 */
	public IFuture<Map<String, Object>> getResultsAsync();
	
	/**
	 *  Get the arguments.
	 *  @return The arguments.
	 */
	public IFuture<Map<String, Object>> getArgumentsAsync();
	
	/**
	 *  Get the exception, if any.
	 *  @return The failure reason for use during cleanup, if any.
	 */
	public IFuture<Exception> getExceptionAsync();
	
	/**
	 * Subscribe to receive results.
	 */
	public ISubscriptionIntermediateFuture<Tuple2<String, Object>> subscribeToResults();

}
