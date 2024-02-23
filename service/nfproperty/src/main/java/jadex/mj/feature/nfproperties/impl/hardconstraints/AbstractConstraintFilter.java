package jadex.mj.feature.nfproperties.impl.hardconstraints;

import jadex.core.IExternalAccess;
import jadex.future.DelegationResultListener;
import jadex.future.ExceptionDelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.model.IAsyncFilter;
import jadex.providedservice.IService;

/**
 * 
 */
public abstract class AbstractConstraintFilter<T> implements IAsyncFilter<T>
{
	/** The component. */
	protected IExternalAccess component;
	
	/** Name of the property being kept constant. */
	protected String propname;
	
	/** The value once it is bound. */
	protected Object value;
	
	/**
	 *  Creates a constant value filter.
	 */
	public AbstractConstraintFilter()
	{
	}
	
	/**
	 *  Creates a constant value filter.
	 */
	public AbstractConstraintFilter(IExternalAccess component, String propname, Object value)
	{
		this.component = component;
		this.propname = propname;
		this.value = value;
	}
	
	/**
	 *  Test if an object passes the filter.
	 *  @return True, if passes the filter.
	 */
	public final IFuture<Boolean> filter(final T service)
	{
		if(getValue() == null)
			return IFuture.TRUE;
		
		throw new UnsupportedOperationException();
		
		/*final Future<Boolean> ret = new Future<Boolean>();
		component.getNFPropertyValue(((IService)service).getServiceId(), propname)
			.addResultListener(new ExceptionDelegationResultListener<Object, Boolean>(ret)
		{
			public void customResultAvailable(Object result)
			{
				doFilter((IService) service, result).addResultListener(new DelegationResultListener<Boolean>(ret));
			}
		});*/
		
//		
//		INFMixedPropertyProvider prov = ((INFMixedPropertyProvider)((IService)service).getExternalComponentFeature(INFPropertyComponentFeature.class));
////		((IService)service).getNFPropertyValue(propname).addResultListener(new IResultListener<Object>()
//		prov.getNFPropertyValue(propname).addResultListener(new IResultListener<Object>()
//		{
//			public void resultAvailable(Object result)
//			{
//				doFilter((IService) service, result).addResultListener(new DelegationResultListener<Boolean>(ret));
//			}
//			
//			public void exceptionOccurred(Exception exception)
//			{
//				ret.setException(exception);
//			}
//		});
		//return ret;
	}
	
	/**
	 *  Test if an object passes the filter.
	 *  @return True, if passes the filter.
	 */
	public abstract IFuture<Boolean> doFilter(IService service, Object value);

	/**
	 *  Gets the valuename.
	 *drag edge areadrag edge area
	 *  @return The valuename.
	 */
	public String getValueName()
	{
		return propname;
	}

	/**
	 *  Sets the valuename.
	 *
	 *  @param valuename The valuename to set.
	 */
	public void setValueName(String valuename)
	{
		this.propname = valuename;
	}

	/**
	 *  Gets the value.
	 *
	 *  @return The value.
	 */
	public Object getValue()
	{
		return value;
	}

	/**
	 *  Sets the value.
	 *
	 *  @param value The value to set.
	 */
	public void setValue(Object value)
	{
		this.value = value;
	}
}
