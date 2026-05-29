package jadex.core.impl;

import java.util.Map;
import java.util.function.Supplier;

import jadex.core.ChangeEvent;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;

/**
 *  Allow providing access to result features without forcing dependency on result feature module.
 *  (I.e. dependency inversion principle).
 */
public interface IResultManager
{
	/**
	 *  Get the results of a component.
	 *  Can be called from outside.
	 */
	public IFuture<Map<String, Object>> getResults(IComponent comp);
	
	/**
	 *  Subscribe to the results of a component.
	 *  Can be called from outside.
	 */
	public ISubscriptionIntermediateFuture<ChangeEvent>	subscribeToResults(IComponent comp);
	
	/**
	 *  Provide access to engine-managed results.
	 *  Should be called on component thread.
	 *  The supplier should always provide a fresh map with copies of values as needed.
	 */
	public void	setResultSupplier(IComponent comp, Supplier<Map<String, Object>> resultsupplier);

	/**
	 *  Set a component result.
	 *  Should be called on component thread.
	 *  @param nocpoy	If true, the value is passed to outside components directly without copying.
	 */
	public void setResult(IComponent comp, String name, Object value, boolean nocpoy);


	/**
	 *  Notify about result changes of a component.
	 *  Should be called on component thread.
	 *  @param nocpoy	If true, the value is passed to outside components directly without copying.
	 */
	public void notifyResult(IComponent comp, ChangeEvent event, boolean nocopy);
}
