package jadex.execution;

import java.util.HashMap;
import java.util.Map;

import jadex.core.ComponentIdentifier;
import jadex.execution.impl.ExecutionFeature;
import jadex.future.ThreadLocalTransferHelper;

/**
 *  Information about a current call.
 *  
 *  Similar to a ThreadLocal in Java but for service calls, i.e.
 *  between different threads and hosts available.
 */
public class Call
{
	//-------- constants --------

	/** The current service calls mapped to threads. */
	protected static final ThreadLocal<Call> CURRENT = new ThreadLocal<Call>();
	
	/** The upcoming service invocations. */
	protected static final ThreadLocal<Call> NEXT = new ThreadLocal<Call>();
	
	/** The upcoming service invocations. */
	protected static final ThreadLocal<Call> LAST = new ThreadLocal<Call>();
	
//	static
//	{
//		ThreadLocalTransferHelper.addThreadLocal(CURRENT);
//		ThreadLocalTransferHelper.addThreadLocal(NEXT);
//		ThreadLocalTransferHelper.addThreadLocal(LAST);
//	}
	
	//-------- attributes --------
	
	/** The calling component. */
	public ComponentIdentifier caller;
	
	/** The service call properties. */
	public Map<String, Object> properties;
	
	protected ExecutionFeature lastmod;
	
	//-------- constructors --------
	
//	static Set<Integer> sprops = Collections.synchronizedSet(new HashSet<Integer>());
	
	/**
	 *  Create a service call info object.
	 */
	protected Call(ComponentIdentifier caller, Map<String, Object> props)
	{
		this.caller	= caller;
		this.properties = props!=null? props: new HashMap<String, Object>();
	}
	
	/**
	 *  Create a service call.
	 */
	public static Call createCall(ComponentIdentifier caller, Map<String, Object> props)
	{
		return new Call(caller, props);
	}
	
	//-------- methods --------
	
	/**
	 *  Get the invocation data for the next service call.
	 */
	public static Call getNextInvocation()
	{
		return NEXT.get();
	}
	
	/**
	 *  Get the service call instance corresponding
	 *  to the current execution context.
	 *  @return The service call instance or null.
	 */
	public static Call getCurrentInvocation()
	{
		return CURRENT.get();
	}
	
	/**
	 *  Get the last service call instance corresponding
	 *  to the current execution context.
	 *  @return The service call instance or null.
	 */
	public static Call getLastInvocation()
	{
		return LAST.get();
	}
	
//	/**
//	 *  Set the properties of the next invocation.
//	 *  @param timeout The timeout.
//	 *  @param realtime The realtime flag.
//	 */
//	public static ServiceCall setInvocationProperties(long timeout, Boolean realtime)
//	{
//		ServiceCall ret = new ServiceCall(IComponentIdentifier.LOCAL.get(), timeout, realtime);
//		INVOCATIONS.set(ret);
//		return ret;
//	}
	
	/**
	 *  Set the properties of the next invocation.
	 *  @param timeout The timeout.
	 *  @param realtime The realtime flag.
	 */
	public static Call getOrCreateNextInvocation()
	{
		return getOrCreateNextInvocation(null);
	}
	
//	/**
//	 *  Get the next invocation if any.
//	 */
//	public static ServiceCall getInvocation0()
//	{
//		return NEXT.get();
//	}
	
	/**
	 *  Get or create the next servicecall for the next invocation. 
	 *  @param timeout The timeout.
	 *  @param realtime The realtime flag.
	 */
	public static Call getOrCreateNextInvocation(Map<String, Object> props)
	{
		Call ret = NEXT.get();
		if(ret==null)
		{
			ret = new Call(findCaller(), props);
			NEXT.set(ret);
		}
		else if(props!=null)
		{
			ret.lastmod	= ExecutionFeature.LOCAL.get();
			ret.properties.putAll(props);
		}
		return ret;
	}
	
	public static ComponentIdentifier findCaller()
	{
		ComponentIdentifier ret = null;
		ExecutionFeature ef = ExecutionFeature.LOCAL.get();
		if(ef!=null && ef.getComponent()!=null)
			ret = ef.getComponent().getId();
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
	 *  Get the timeout value.
	 *  @return The timeout value or -1.
	 * /
	public long	getTimeout()
	{
		return properties.containsKey(TIMEOUT)? ((Long)properties.get(TIMEOUT)).longValue():
			properties.containsKey(DEFTIMEOUT)? ((Long)properties.get(DEFTIMEOUT)).longValue() : SUtil.DEFTIMEOUT;
	}*/
	
	/**
	 *  Test if the user has set a timeout.
	 *  @return True, if the user has set a timeout.
	 * /
	public boolean hasUserTimeout()
	{
		return properties.containsKey(TIMEOUT);
	}*/
	
	/**
	 *  Set the timeout.
	 *  @param to The timeout.
	 * /
	public void setTimeout(long to)
	{
//		if(((String)properties.get("method")).indexOf("service")!=-1)
//			System.out.println("sdfjbsdfjk");
		lastmod	= IComponentIdentifier.LOCAL.get();
		properties.put(TIMEOUT, Long.valueOf(to));
	}*/
	
	/**
	 *  Get the realtime flag.
	 *  @return True, if the timeout is a real time (i.e. system time)
	 *    instead of platform time. 
	 */
//	public Boolean	getRealtime()
//	{
//		return (Boolean)properties.get(REALTIME);
//	}
	
	/**
	 *  Set the realtime property.
	 */
//	public void setRealtime(Boolean realtime)
//	{
//		lastmod	= IComponentIdentifier.LOCAL.get();
//		properties.put(REALTIME, realtime);
//	}
	
	/**
	 *  Get the realtime flag.
	 *  @return True, if the timeout is a real time (i.e. system time)
	 *    instead of platform time. 
	 */
//	public boolean isRealtime()
//	{
//		return getRealtime().booleanValue();
//	}
	
	/**
	 *  Test if a call is remote.
	 * /
	public boolean isRemoteCall(IComponentIdentifier callee)
	{
		IComponentIdentifier platform = callee.getRoot();
		return caller==null? false: !caller.getRoot().equals(platform);
	}*/
	
//	/**
//	 *  Get the cause.
//	 *  @return The cause.
//	 */
//	public Cause getCause()
//	{
////		if(properties.get(CAUSE)!=null && !(properties.get(CAUSE) instanceof Cause))
////		{
////			System.out.println("sdmyb");
////		}
//		return (Cause)properties.get(CAUSE);
//	}
//	
//	/**
//	 *  Set the cause.
//	 *  @param cause The cause.
//	 */
//	public void setCause(Cause cause)
//	{
//		lastmod	= IComponentIdentifier.LOCAL.get();
//		properties.put(CAUSE, cause);
//	}
	
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
//		if(TIMEOUT.equals(name))
//		{
//			if(properties.get("method")!=null && ((String)properties.get("method")).indexOf("service")!=-1)
//				System.out.println("setting tout: "+val);
//			else if(properties.get("method")==null)
//				System.out.println("setting unknown tout: "+val);
//		}
		//lastmod	= IComponentIdentifier.LOCAL.get();
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
	 *  Get a shallow clone from all props.
	 *  @return The properties clone.
	 */
	public Map<String, Object> getPropertiesClone()
	{
		return new HashMap<String, Object>(properties);
	}
	
	/**
	 *  Create a service call.
	 *  @param caller	The calling component. 
	 *  @param props	The properties.
	 * /
	public static Call createServiceCall(ComponentIdentifier caller, Map<String, Object> props)
	{
		return createServiceCall(caller, props);
	}*/

	/**
	 *  Set the current service call.
	 *  @param call	The service call.
	 */
	public static void	setCurrentInvocation(Call call)
	{
		CURRENT.set(call);
	}

	/**
	 *  Remove the current service call.
	 */
	public static void	resetCurrentInvocation()
	{
		CURRENT.set(null);
	}
	
	/**
	 *  Reset the invocation data for the next service call.
	 */
	public static void	setNextInvocation(Call call)
	{
		NEXT.set(call);
	}

	/**
	 *  Reset the invocation data for the next call.
	 */
	public static void	resetNextInvocation()
	{
		NEXT.set(null);
	}
	
	/**
	 *  Reset the invocation data for the last call.
	 */
	public static void	setLastInvocation(Call call)
	{
		LAST.set(call);
	}

	/**
	 *  Reset the invocation data for the last service call.
	 */
	public static void	resetLastInvocation()
	{
		LAST.set(null);
	}
	
	/**
	 *  Make LAST = CURRENT; CURRENT=NEXT; NEXT=null
	 */
	public static void roll()
	{
		LAST.set(CURRENT.get());
		CURRENT.set(NEXT.get());
		NEXT.set(null);
	}

	/** 
	 *  Get the string represntation.
	 */
	public String toString()
	{
		return "Call(caller=" + caller + ", properties=" + properties+ ")";
	}
}
