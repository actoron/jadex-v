package jadex.bdi.runtime;

import jadex.bdi.runtime.impl.BDIAgent;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.execution.IExecutionFeature;

/**
 *  Interface for injecting agent methods into pojos.
 */
public interface IBDIAgent extends IExecutionFeature, IBDIAgentFeature
{
	/**
	 *  Create a BDI agent from a pojo object.
	 *  Object needs to provide soem initialization fields.
	 *  @see BDIBaseAgent
	 */
	public static IComponentHandle create(Object pojo)
	{
		return BDIAgent.create(pojo);
	}
	
	/**
	 *  Create a BDI agent from a pojo object.
	 *  Object needs to provide soem initialization fields.
	 *  @see BDIBaseAgent
	 */
	public static IComponentHandle	create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return BDIAgent.create(pojo, cid, app);
	}

	/**
	 *  Create a BDI agent from a class to be enhanced.
	 */
	public static IComponentHandle create(String classname)
	{
		return BDIAgent.create(classname);
	}
	
	/**
	 *  Create a BDI agent from a class to be enhanced.
	 */
	public static IComponentHandle	create(String classname, ComponentIdentifier cid, Application app)
	{
		return BDIAgent.create(classname, cid, app);
	}

	/**
	 *  Create a BDI agent from a class to be enhanced and optional args.
	 */
	public static IComponentHandle create(BDICreationInfo info)
	{
		return BDIAgent.create(info);
	}
	
	/**
	 *  Create a BDI agent from a class to be enhanced and optional args.
	 */
	public static IComponentHandle	create(BDICreationInfo info, ComponentIdentifier cid, Application app)
	{
		return BDIAgent.create(info, cid, app);
	}
}
