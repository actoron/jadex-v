package jadex.providedservice.impl.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractServiceInvocationHandler  
{
	/** The raw proxy type (i.e. no proxy). */
	public static final String	PROXYTYPE_RAW	= "raw";
	
	/** The direct proxy type (supports custom interceptors, but uses caller thread). */
	public static final String	PROXYTYPE_DIRECT	= "direct";
	
	/** The (default) decoupled proxy type (decouples from caller thread to component thread). */
	public static final String	PROXYTYPE_DECOUPLED	= "decoupled";

	/** The list of interceptors. */
	public final List<IServiceInvocationInterceptor> interceptors = Collections.synchronizedList(new ArrayList<IServiceInvocationInterceptor>());
	
	public abstract Object getDomainService();
	
	public abstract Object getService();
	
	public abstract boolean isRequired();
	
	/**
	 *  Add an interceptor.
	 *  
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public void addFirstServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		//if(interceptors==null)
		//	interceptors = new ArrayList<IServiceInvocationInterceptor>();
		interceptors.add(0, interceptor);
	}
	
	/**
	 *  Add an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public void addServiceInterceptor(IServiceInvocationInterceptor interceptor, int pos)
	{
		//if(interceptors==null)
		//	interceptors = new ArrayList<IServiceInvocationInterceptor>();
		// Hack? -1 for default position one before method invocation interceptor
		interceptors.add(pos>-1? pos: interceptors.size()-1, interceptor);
	}
	
	/**
	 *  Add an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public void addServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		addServiceInterceptor(interceptor, -1);
	}
	
	/**
	 *  Remove an interceptor.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public void removeServiceInterceptor(IServiceInvocationInterceptor interceptor)
	{
		//if(interceptors!=null)
		interceptors.remove(interceptor);
	}
	
	/**
	 *  Get interceptors.
	 *  Must be synchronized as invoke() is called from arbitrary threads.
	 */
	public IServiceInvocationInterceptor[] getInterceptors()
	{
		IServiceInvocationInterceptor[] ret = interceptors==null || interceptors.size()==0? null://new IServiceInvocationInterceptor[]{fallback}: 
			(IServiceInvocationInterceptor[])interceptors.toArray(new IServiceInvocationInterceptor[interceptors.size()]);
	
		//System.out.println("intercepts: "+this+" "+ret.length+" "+interceptors.hashCode());
		
		return ret;
	}
}
