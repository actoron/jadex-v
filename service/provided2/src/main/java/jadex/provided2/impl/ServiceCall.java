package jadex.provided2.impl;

import java.util.HashMap;
import java.util.Map;

import jadex.core.ComponentIdentifier;
import jadex.execution.impl.ExecutionFeature;
import jadex.future.ThreadLocalTransferHelper;


/**
 *  Information about a current service call.
 *  
 *  Similar to a ThreadLocal in Java but for service calls, i.e.
 *  between different threads and hosts available.
 */
public class ServiceCall
{
	//-------- constants --------
	
	/** The inherit constant. */
	public static final String INHERIT = "inherit";
	
	/** The current service calls mapped to threads. */
	protected static final ThreadLocal<ServiceCall> CURRENT = new ThreadLocal<ServiceCall>();
	
	/** The upcoming service invocations. */
	protected static final ThreadLocal<ServiceCall> NEXT = new ThreadLocal<ServiceCall>();
	
	/** The upcoming service invocations. */
	protected static final ThreadLocal<ServiceCall> LAST = new ThreadLocal<ServiceCall>();

	static
	{
		ThreadLocalTransferHelper.addThreadLocal(CURRENT);
		ThreadLocalTransferHelper.addThreadLocal(NEXT);
		ThreadLocalTransferHelper.addThreadLocal(LAST);
	}
	
	//-------- attributes --------
	
	/** The calling component. */
	public ComponentIdentifier caller;
	
	/** The service call properties. */
	public Map<String, Object> properties;
	
	protected ExecutionFeature lastmod;
	
	//-------- constructors --------
	
	/**
	 *  Create a service call info object.
	 */
	protected ServiceCall(ComponentIdentifier caller, Map<String, Object> props)
	{
		this.caller	= caller;
		this.properties = props!=null? props: new HashMap<String, Object>();
	}
	
	/**
	 *  Create a service call.
	 */
	protected static ServiceCall createServiceCall(ComponentIdentifier caller, Map<String, Object> props)
	{
		return new ServiceCall(caller, props);
	}
	
	//-------- methods --------
	
	/**
	 *  Get the invocation data for the next service call.
	 */
	public static ServiceCall getNextInvocation()
	{
		return NEXT.get();
	}
	
	/**
	 *  Get the service call instance corresponding
	 *  to the current execution context.
	 *  @return The service call instance or null.
	 */
	public static ServiceCall getCurrentInvocation()
	{
		return CURRENT.get();
	}
	
	/**
	 *  Get the last service call instance corresponding
	 *  to the current execution context.
	 *  @return The service call instance or null.
	 */
	public static ServiceCall getLastInvocation()
	{
		return LAST.get();
	}
		
	/**
	 *  Set the properties of the next invocation.
	 *  @param timeout The timeout.
	 *  @param realtime The realtime flag.
	 */
	public static ServiceCall getOrCreateNextInvocation()
	{
		return getOrCreateNextInvocation(null);
	}
		
	/**
	 *  Get or create the next servicecall for the next invocation. 
	 *  @param timeout The timeout.
	 *  @param realtime The realtime flag.
	 */
	public static ServiceCall getOrCreateNextInvocation(Map<String, Object> props)
	{
		ServiceCall ret = NEXT.get();
		if(ret==null)
		{
			ret = new ServiceCall(ExecutionFeature.LOCAL.get().getComponent().getId(), props);
			
			
			NEXT.set(ret);
		}
		else if(props!=null)
		{
			
			ret.lastmod	= ExecutionFeature.LOCAL.get();
			ret.properties.putAll(props);
		}
		return ret;
	}
	
	/**
	 *  Get the caller component.
	 *  @return The caller component.
	 */
	public ComponentIdentifier getCaller()
	{
		return caller;
	}
		
	/**
	 *  Get a property.
	 *  @param name The property name.
	 *  @return The property.
	 */
	public Object getProperty(String name)
	{
		return properties.get(name);
	}
	
	/**
	 *  Set a property.
	 *  @param name The property name.
	 *  @param val The property value.
	 */
	public void setProperty(String name, Object val)
	{
		this.properties.put(name, val);
	}
	
	/**
	 *  Get a shallow clone from all props.
	 *  @return The properties clone.
	 */
	public Map<String, Object> getPropertiesClone()
	{
		return new HashMap<String, Object>(properties);
	}

	/** 
	 *  Get the string represntation.
	 */
	public String toString()
	{
		return "ServiceCall(caller=" + caller + ", properties=" + properties+ ")";
	}
}
