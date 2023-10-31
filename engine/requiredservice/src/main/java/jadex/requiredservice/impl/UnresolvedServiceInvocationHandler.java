package jadex.requiredservice.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import jadex.common.SReflect;
import jadex.core.impl.Component;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.providedservice.IService;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.providedservice.impl.service.interceptors.FutureFunctionality;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Lazy service proxy that resolves a service via a search command.
 */
public class UnresolvedServiceInvocationHandler implements InvocationHandler
{
	/** The component. */
	protected Component ia;
	
	/** The service. */
	protected IService delegate;
	
	/** The service being acquired. */
	IFuture<IService> delegatefut;
	
	/** The search query for a lazy proxy. */
	protected ServiceQuery<?> query;

	/**
	 *  Create a new invocation handler.
	 */
	public UnresolvedServiceInvocationHandler(Component ia, ServiceQuery<?> query)
	{
		this.ia = ia;
		this.query = query;
	}
	
	/**
	 *  Called on method invocation.
	 */
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
	{
		if (delegate == null)
		{
			if (delegatefut == null)
			{
				@SuppressWarnings("unchecked")
				IFuture<IService> fut = (IFuture<IService>) ia.getFeature(IRequiredServiceFeature.class).searchService(query, 0);
				fut.then(serv ->
				{
					delegate = serv;
					delegatefut = null;
				}).catchEx(e -> delegatefut = null);
				delegatefut = (IFuture<IService>) fut;
			}
			if (!SReflect.isSupertype(IFuture.class, method.getReturnType()))
			{
				// Method is synchronous, no choice...
				IService serv = delegatefut.get();
				return method.invoke(serv, args);
			}
			else
			{
				@SuppressWarnings("unchecked")
				Future<Object> ret = (Future<Object>)FutureFunctionality.getDelegationFuture(method.getReturnType(), new FutureFunctionality());
				delegatefut.addResultListener(new IResultListener<IService>()
				{
					public void resultAvailable(IService result)
					{
						try
						{
							@SuppressWarnings("unchecked")
							IFuture<Object>	origret = (IFuture<Object>) method.invoke(result, args);
							origret.delegateTo(ret);
						}
						catch (Exception e)
						{
							ret.setException(e);
						}
					}
					
					public void exceptionOccurred(Exception exception)
					{
						ret.setException(exception);
					}
				});
				return ret;
			}
		}
		
		return method.invoke(delegate, args);
	}
}
