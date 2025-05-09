package jadex.providedservice.impl.service;

import java.util.HashMap;
import java.util.Map;

import jadex.core.ComponentIdentifier;
import jadex.core.impl.ComponentManager;
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
	
	/** The security infos constant. */
	public static final String SECURITY_INFOS = "secinfos";
	
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
	 */
	public static ServiceCall getOrCreateNextInvocation()
	{
		return getOrCreateNextInvocation(null);
	}
		
	/**
	 *  Get or create the next servicecall for the next invocation. 
	 *  @param props The properties.
	 */
	public static ServiceCall getOrCreateNextInvocation(Map<String, Object> props)
	{
		ServiceCall ret = NEXT.get();
		if(ret==null)
		{
			// Set id to global runner if called from non-component thread
			ComponentIdentifier	id	= ExecutionFeature.LOCAL.get()!=null
				? ExecutionFeature.LOCAL.get().getComponent().getId()
				: ComponentManager.get().getGlobalRunner().getId();
			ret = new ServiceCall(id, props);
			
			
			NEXT.set(ret);
		}
		else if(props!=null)
		{
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
	 *  Remove a property.
	 *  @param name The property name.
	 * /
	public void removeProperty(String name)
	{
		lastmod	= IComponentIdentifier.LOCAL.get();
		this.properties.remove(name);
	}*/

	/**
	 *  Get all props directly. Do not use unless
	 *  you are sure this is what you need. Consider
	 *  getPropertiesClone() for a shallow copy.
	 *
	 *  @return The properties.
	 */
	public Map<String, Object> getProperties()
	{
		return properties;
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
