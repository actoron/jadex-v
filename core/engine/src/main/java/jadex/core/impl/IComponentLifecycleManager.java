package jadex.core.impl;

import java.util.Map;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;

/**
 *  Managing POJOs by creating/running/terminating corresponding components.
 */
public interface IComponentLifecycleManager
{
	/**
	 *  Check if a POJO can be handled.
	 */
	public boolean	isCreator(Object pojo);

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
	public Map<String, Object> getResults(Object pojo);
}