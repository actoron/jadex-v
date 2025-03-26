package jadex.core.impl;

import java.util.Map;

import jadex.common.NameValue;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;

/**
 *  Managing POJOs by creating/running/terminating corresponding components.
 */
public interface IComponentLifecycleManager
{
	/**
	 *  Check if a POJO can be handled.
	 *  
	 *  @return Priority: -1: cannot create, 0: fallback, 1: normal kernel, 2: extension of normal kernel, 3:...
	 */
	public int	isCreator(Class<?> pojoclazz);

	/**
	 *  Create a component for a POJO
	 */
	public IComponentHandle	create(Object pojo, ComponentIdentifier cid, Application app);

//	/**
//	 *  Run a component with a POJO and fetch the results, if any.
//	 */
//	public default Object	run(Object pojo, ComponentIdentifier cid)
//	{
//		throw new UnsupportedOperationException("No run() handler implemented for "+pojo.getClass().getName());
//	}

	/**
	 *  Execute termination code for the given component
	 */
	public void terminate(IComponent component);
	
	/**
	 *  Fetch the result(s) of the POJO.
	 */
	public default Map<String, Object> getResults(IComponent component)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Listen to results of the pojo.
	 *  @throws UnsupportedOperationException when subscription is not supported
	 */
	public default ISubscriptionIntermediateFuture<NameValue> subscribeToResults(IComponent component)
	{
		return new SubscriptionIntermediateFuture<NameValue>(new UnsupportedOperationException());
	}
}