package jadex.bdi.runtime;

import jadex.bdi.runtime.impl.BDIAgent;
import jadex.core.ComponentIdentifier;
import jadex.core.IExternalAccess;
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
	public static IExternalAccess create(Object pojo)
	{
		return BDIAgent.create(pojo);
	}
	
	/**
	 *  Create a BDI agent from a pojo object.
	 *  Object needs to provide soem initialization fields.
	 *  @see BDIBaseAgent
	 */
	public static IExternalAccess	create(Object pojo, ComponentIdentifier cid)
	{
		return BDIAgent.create(pojo, cid);
	}

	/**
	 *  Create a BDI agent from a class to be enhanced.
	 */
	public static IExternalAccess create(String classname)
	{
		return BDIAgent.create(classname);
	}
	
	/**
	 *  Create a BDI agent from a class to be enhanced.
	 */
	public static IExternalAccess	create(String classname, ComponentIdentifier cid)
	{
		return BDIAgent.create(classname, cid);
	}

	/**
	 *  Create a BDI agent from a class to be enhanced and optional args.
	 */
	public static IExternalAccess create(BDICreationInfo info)
	{
		return BDIAgent.create(info);
	}
	
	/**
	 *  Create a BDI agent from a class to be enhanced and optional args.
	 */
	public static IExternalAccess	create(BDICreationInfo info, ComponentIdentifier cid)
	{
		return BDIAgent.create(info, cid);
	}
}
