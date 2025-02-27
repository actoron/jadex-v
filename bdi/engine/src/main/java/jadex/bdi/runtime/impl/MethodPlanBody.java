package jadex.bdi.runtime.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jadex.bdi.model.MElement;
import jadex.common.SAccess;

/**
 *  Implementation of a method as a plan body.
 */
public class MethodPlanBody extends AbstractPlanBody
{
	//-------- attributes --------
	
	/** The method. */
	protected Method body;
	
	/** The agent/capability object. */
	protected Object agent;
	
	//-------- constructors --------
	
	/**
	 *  Create a new plan body.
	 */
	public MethodPlanBody(RPlan rplan, Method body)
	{
		super(rplan);
		this.body = body;
		String	pname	= rplan.getModelElement().getName();
		String	capaname	= pname.indexOf(MElement.CAPABILITY_SEPARATOR)==-1
			? null : pname.substring(0, pname.lastIndexOf(MElement.CAPABILITY_SEPARATOR));
		this.agent	= IInternalBDIAgentFeature.get().getCapabilityObject(capaname);
	}
	
	//-------- methods --------
	
	/**
	 *  Invoke the body.
	 */
	public Object invokeBody(Object[] params)
	{
		try
		{
			SAccess.setAccessible(body, true);
			return body.invoke(agent, params);
		}
		catch(Exception e)
		{
			Throwable	t	= e;
			if(e instanceof InvocationTargetException)
			{
				t	= ((InvocationTargetException)e).getTargetException();
			}
			if(t instanceof RuntimeException)
			{
				throw (RuntimeException)t;
			}
			else if(t instanceof Error)
			{
				throw (Error)t;
			}
			else
			{
				throw new RuntimeException(t);
			}
		}
	}
	
	public Object invokePassed(Object[] params)
	{
		return null;
	}

	public Object invokeFailed(Object[] params)
	{
		return null;
	}

	public Object invokeAborted(Object[] params)
	{
		return null;
	}
	
	public Object invokeFinished(Object[] params)
	{
		return null;
	}

	/**
	 *  Get the body parameter types.
	 */
	public Class<?>[] getBodyParameterTypes()
	{
		return body.getParameterTypes();
	}

	public Class<?>[] getPassedParameterTypes()
	{
		return null;
	}

	public Class<?>[] getFailedParameterTypes()
	{
		return null;
	}

	public Class<?>[] getAbortedParameterTypes()
	{
		return null;
	}
	
	public Class<?>[] getFinishedParameterTypes()
	{
		return null;
	}
}