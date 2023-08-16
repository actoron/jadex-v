package jadex.enginecore.service.component.interceptors;

import java.lang.reflect.Method;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.component.INFPropertyComponentFeature;
import jadex.enginecore.nonfunctional.INFMixedPropertyProvider;
import jadex.enginecore.nonfunctional.INFRPropertyProvider;
import jadex.enginecore.service.IServiceIdentifier;
import jadex.enginecore.service.component.ServiceInvocationContext;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Delegates 'getRequiredServicePropertyProvider()' calls
 *  to the underlying component.
 */
public class NFRequiredServicePropertyProviderInterceptor extends ComponentThreadInterceptor
{
	protected static final Method METHOD;
	
	static
	{
		try
		{
			METHOD = INFRPropertyProvider.class.getMethod("getRequiredServicePropertyProvider", new Class[0]);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	//-------- methods --------

	/** The service indentifier. */
	protected IServiceIdentifier sid;

	/**
	 *  Create a new interceptor.
	 */
	public NFRequiredServicePropertyProviderInterceptor(IInternalAccess component, IServiceIdentifier sid)
	{
		super(component);
		this.sid = sid;
	}
	
	/**
	 *  Test if the interceptor is applicable.
	 *  @return True, if applicable.
	 */
	public boolean isApplicable(ServiceInvocationContext context)
	{
		return super.isApplicable(context) && context.getMethod().equals(METHOD);
	}
	
	/**
	 *  Execute the interceptor.
	 *  @param context The invocation context.
	 */
	public IFuture<Void> execute(ServiceInvocationContext sic)
	{
//		INFMixedPropertyProvider res = component.getRequiredServicePropertyProvider((IServiceIdentifier)sic.getArgumentArray()[0]);
		INFMixedPropertyProvider res = getComponent().getFeature(INFPropertyComponentFeature.class).getRequiredServicePropertyProvider(sid);
		sic.setResult(new Future<INFMixedPropertyProvider>(res));
		return IFuture.DONE;
	}
}