package jadex.nfproperty.sensor.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jadex.bytecode.ProxyFactory;
import jadex.common.MethodInfo;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.nfproperty.sensor.time.TimedProperty;
import jadex.providedservice.IMethodInvocationListener;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;

/**
 *  Property for the overall execution time of a method or a service.
 */
public class ExecutionTimeProperty extends TimedProperty
{
	/** The name of the property. */
	public static final String NAME = "waiting time";
	
	/** The service identifier. */
	protected IServiceIdentifier sid;
	
	/** The listener. */
	protected IMethodInvocationListener listener;
	
	/** The method info. */
	protected MethodInfo method;
	
	/**
	 *  Create a new property.
	 */
	public ExecutionTimeProperty(final IComponent comp, IService service, MethodInfo method)
	{
		super(NAME, comp, true);
		this.method = method;
		
		//System.out.println("creating exetimeprop:"+service.getServiceId()+" "+method);
		
		if(service!=null)
		{
			this.sid = service.getServiceId();
		
			if(ProxyFactory.isProxyClass(service.getClass()))
			{
				listener = new IMethodInvocationListener()
				{
					Map<Object, Long> times = new HashMap<Object, Long>();
					
					public void methodCallStarted(Object proxy, Method method, Object[] args, Object callid)
					{
						times.put(callid, Long.valueOf(comp.getFeature(IExecutionFeature.class).getTime()));
					}
					
					public void methodCallFinished(Object proxy, Method method, Object[] args, Object callid)
					{
						Long start = times.remove(callid);
						// May happen that property is added during ongoing call
						if(start!=null)
						{
							long dur = comp.getFeature(IExecutionFeature.class).getTime() - start.longValue();
							setValue(dur);
						}
					}
				};
	//			System.out.println("installing lis: "+comp.getComponentIdentifier().getName());
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
//		System.out.println("Set value: "+value);
		
		// ema calculatio: EMAt = EMAt-1 +(SF*(Ct-EMAt-1)) SF=2/(n+1)
		if(this.value!=null && value!=null)
		{
			double sf = 2d/(10d+1); // 10 periods per default
			double delta = value-this.value;
			value = Long.valueOf((long)(this.value+sf*delta));
		}
		
		if(value!=null)
		{
//			System.out.println("Setting exp value: "+value);
			super.setValue(value);
		}
	}
	
	/**
	 *  Property was removed and should be disposed.
	 */
	public IFuture<Void> dispose()
	{
		comp.getFeature(IProvidedServiceFeature.class).removeMethodInvocationListener(sid, method, listener);
		return IFuture.DONE;
	}
}
