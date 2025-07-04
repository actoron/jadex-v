package jadex.nfproperty.sensor.service;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import jadex.common.MethodInfo;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.nfproperty.impl.NFPropertyMetaInfo;
import jadex.nfproperty.impl.SimpleValueNFProperty;
import jadex.providedservice.IMethodInvocationListener;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;


/**
 *  Property for the waitqueue length (in calls) of a method or a service.
 */
public class WaitqueueProperty extends SimpleValueNFProperty<Integer, Void>
{
	/** The name of the property. */
	public static final String NAME = "wait queue length";
	
	/** The service identifier. */
	protected IServiceIdentifier sid;
	
	/** The listener. */
	protected IMethodInvocationListener listener;
	
	/** The method info. */
	protected MethodInfo method;
	
	/**
	 *  Create a new property.
	 */
	public WaitqueueProperty(IComponent comp, IService service, MethodInfo method)
	{
		super(comp, new NFPropertyMetaInfo(NAME, int.class, Void.class, true, -1, true, null));
		this.method = method;
		this.sid = service.getServiceId();
		
		if(Proxy.isProxyClass(service.getClass()))
		{
			listener = new IMethodInvocationListener()
			{
				int cnt = 0;
				
				public void methodCallStarted(Object proxy, Method method, Object[] args, Object callid)
				{
//					System.out.println("started: "+method+" "+cnt);
					setValue(Integer.valueOf(++cnt));
				}
				
				public void methodCallFinished(Object proxy, Method method, Object[] args, Object callid)
				{
//					System.out.println("ended: "+method+" "+cnt);
					if(cnt>0)
						--cnt;
					setValue(Integer.valueOf(cnt));
				}
			};
			
			comp.getFeature(IProvidedServiceFeature.class).addMethodInvocationListener(sid, method, listener);
		}
		else
		{
			throw new RuntimeException("Cannot install wait queue listener hook.");
		}
	}
	
	/**
	 *  Measure the value.
	 */
	public Integer measureValue()
	{
		return null;
//		throw new UnsupportedOperationException();
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
