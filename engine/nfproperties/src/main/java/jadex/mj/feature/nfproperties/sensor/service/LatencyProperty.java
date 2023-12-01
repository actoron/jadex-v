package jadex.mj.feature.nfproperties.sensor.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jadex.bridge.service.component.IProvidedServicesFeature;
import jadex.bytecode.ProxyFactory;
import jadex.common.MethodInfo;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.mj.feature.nfproperties.sensor.time.TimedProperty;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.service.ServiceInvocationContext;

/**
 *  Property for the overall latency a service call.
 *  
 *  Computes endtime-starttime-methodexetime
 *  
 *  The local method execution time is measured locally and
 *  stored as non-functional property in the call context.
 */
public class LatencyProperty extends TimedProperty
{
	/** The name of the property. */
	public static final String NAME = "latency";
	
	/** The service identifier. */
	protected IServiceIdentifier sid;
	
	/** The listener. */
	protected IMethodInvocationListener listener;
	
	/** The method info. */
	protected MethodInfo method;
	
	/**
	 *  Create a new property.
	 */
	public LatencyProperty(IComponent comp, IService service, MethodInfo method)
	{
		super(NAME, comp, true);
		this.method = method;
		
		// Necessary for unbound required service property to fetch meta info :-(
		if(service!=null)
		{
			this.sid = service.getServiceId();
		
			if(ProxyFactory.isProxyClass(service.getClass()))
			{
				listener = new UserMethodInvocationListener(new IMethodInvocationListener()
				{
					Map<Object, Long> times = new HashMap<Object, Long>();
					
					public void methodCallStarted(Object proxy, Method method, Object[] args, Object callid, ServiceInvocationContext context)
					{
						times.put(callid, Long.valueOf(System.currentTimeMillis()));
					}
					
					public void methodCallFinished(Object proxy, Method method, Object[] args, Object callid, ServiceInvocationContext context)
					{
						Long start = times.remove(callid);
						// May happen that property is added during ongoing call
						if(start!=null)
						{
							if(context!=null)
							{
								ServiceInvocationContext sic = (ServiceInvocationContext)context;
								if(sic.getNextServiceCall()!=null)
								{
									Long exe = (Long)sic.getNextServiceCall().getProperty("__duration");
									if(exe!=null)
									{
										long dur = System.currentTimeMillis() - start.longValue() - exe.longValue();
//										System.out.println("latency is: "+dur);
										setValue(dur);
									}
								}
							}
							else
							{
								System.out.println("no context");
							}
						}
					}
				});
				comp.getFeature(IProvidedServiceFeature.class).addMethodInvocationListener(service.getServiceId(), method, listener);
			}
			else
			{
				throw new RuntimeException("Cannot install waiting time listener hook.");
			}
		}
	}
	
	/**
	 *  Measure the value.
	 */
	public Long measureValue()
	{
		return null;
//		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Set the value.
	 */
	public void setValue(Long value) 
	{
		// ema calculatio: EMAt = EMAt-1 +(SF*(Ct-EMAt-1)) SF=2/(n+1)
		if(this.value!=null && value!=null)
		{
			double sf = 2d/(10d+1); // 10 periods per default
			double delta = value-this.value;
			value = Long.valueOf((long)(this.value+sf*delta));
		}
		
		if(value!=null)
		{
//			System.out.println("Setting value: "+value);
			super.setValue(value);
		}
	}
	
	/**
	 *  Property was removed and should be disposed.
	 */
	public IFuture<Void> dispose()
	{
		comp.getFeature(IProvidedServicesFeature.class).removeMethodInvocationListener(sid, method, listener);
		return super.dispose();
	}
}
