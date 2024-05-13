package jadex.bdi.runtime;

import jadex.bdi.runtime.impl.BDIAgent;
import jadex.core.ComponentIdentifier;
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
	public static void create(Object pojo)
	{
		BDIAgent.create(pojo);
	}
	
	/**
	 *  Create a BDI agent from a pojo object.
	 *  Object needs to provide soem initialization fields.
	 *  @see BDIBaseAgent
	 */
	public static void	create(Object pojo, ComponentIdentifier cid)
	{
		BDIAgent.create(pojo, cid);
	}

	/**
	 *  Create a BDI agent from a class to be enhanced.
	 */
	public static void create(String classname)
	{
		BDIAgent.create(classname);
	}
	
	/**
	 *  Create a BDI agent from a class to be enhanced.
	 */
	public static void	create(String classname, ComponentIdentifier cid)
	{
		BDIAgent.create(classname, cid);
	}

	/**
	 *  Create a BDI agent from a class to be enhanced and optional args.
	 */
	public static void create(BDICreationInfo info)
	{
		BDIAgent.create(info);
	}
	
	/**
	 *  Create a BDI agent from a class to be enhanced and optional args.
	 */
	public static void	create(BDICreationInfo info, ComponentIdentifier cid)
	{
		BDIAgent.create(info, cid);
	}
}
