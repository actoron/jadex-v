package jadex.nfproperty.impl.search;

import jadex.common.MethodInfo;
import jadex.core.IComponentHandle;
import jadex.future.ExceptionDelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.nfproperty.INFPropertyFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;

/**
 *  Basic evaluator implementation for service and methods.
 */
public abstract class BasicEvaluator<T> implements IServiceEvaluator
{
	/** The component. */
	protected IComponentHandle component;
	
	/** The property name. */
	protected String propertyname;
	
	/** The method info. */
	protected MethodInfo methodinfo;
	
	/** The unit. */
	protected Object unit;
	
	/** The required flag. */
	protected boolean required;
	
	/**
	 *  Create a new evaluator.
	 *  @param propertyname The property name.
	 */
	public BasicEvaluator(IComponentHandle component, String propertyname)
	{
		this(component, propertyname, null, null, false);
	}
	
	/**
	 *  Create a new evaluator.
	 *  @param propertyname The property name.
	 */
	public BasicEvaluator(IComponentHandle component, String propertyname, Object unit)
	{
		this(component, propertyname, null, unit, false);
	}
	
	/**
	 *  Create a new evaluator.
	 *  @param propertyname The property name.
	 */
	public BasicEvaluator(IComponentHandle component, String propertyname, MethodInfo mi)
	{
		this(component, propertyname, mi, null, false);
	}
	
	/**
	 *  Create a new evaluator.
	 *  @param propertyname The property name.
	 *  @param methodinfo The method.
	 *  @param unit The unit.
	 */
	public BasicEvaluator(IComponentHandle component, String propertyname, MethodInfo methodinfo, Object unit, boolean required)
	{
		this.component = component;
		this.propertyname = propertyname;
		this.methodinfo = methodinfo;
		this.unit = unit;
		this.required = required;
	}
	
	/**
	 * 
	 * @param propertyvalue
	 * @return
	 */
	public abstract double calculateEvaluation(T propertyvalue);
	
	/**
	 *  Evaluate the service of method.
	 */
	public IFuture<Double> evaluate(IService service)
	{
		final Future<Double> ret = new Future<Double>();
		final IResultListener<T> listener = new ExceptionDelegationResultListener<T, Double>(ret)
		{
			public void customResultAvailable(T result)
			{
				ret.setResult(calculateEvaluation(result));
			}
		};
		
		getPropertyValue(((IService)service).getServiceId()).addResultListener(listener);
		
		return ret;
	}
	
	/**
	 *  Get the property value based on the provider.
	 */
	protected IFuture<T> getPropertyValue(final IServiceIdentifier sid)
	{
		final Future<T> ret = new Future<T>();
		
		if(required)
		{
			if(methodinfo!=null)
			{
				if(unit!=null)
				{
					component.scheduleStep(agent ->
					{
						IFuture<T> fut = agent.getFeature(INFPropertyFeature.class).getRequiredMethodNFPropertyValue(sid, methodinfo, propertyname, unit);
						fut.delegateTo(ret);
					});
				}
				else
				{
					component.scheduleStep(agent ->
					{
						IFuture<T> fut = agent.getFeature(INFPropertyFeature.class).getRequiredMethodNFPropertyValue(sid, methodinfo, propertyname);
						fut.delegateTo(ret);
					});
				}
			}
			else
			{
				if(unit!=null)
				{
					component.scheduleStep(agent ->
					{
						IFuture<T> fut = agent.getFeature(INFPropertyFeature.class).getRequiredNFPropertyValue(sid, propertyname, unit);
						fut.delegateTo(ret);
					});
				}
				else
				{
					component.scheduleStep(agent ->
					{
						IFuture<T> fut = agent.getFeature(INFPropertyFeature.class).getRequiredNFPropertyValue(sid, propertyname);
						fut.delegateTo(ret);
					});
				}
			}
		}
		else
		{
			
			IComponentHandle exta = component.getExternalAccess(sid.getProviderId());
			exta.scheduleStep(agent ->
			{
				if(methodinfo!=null)
				{
					if(unit!=null)
					{
						IFuture<T> fut = agent.getFeature(INFPropertyFeature.class).getMethodNFPropertyValue(sid, methodinfo, propertyname, unit);
						fut.delegateTo(ret);
					}
					else
					{
						IFuture<T> fut = agent.getFeature(INFPropertyFeature.class).getMethodNFPropertyValue(sid, methodinfo, propertyname);
						fut.delegateTo(ret);
					}
				}
				else
				{
					if(unit!=null)
					{
						IFuture<T> fut = agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(sid, propertyname, unit);
						fut.delegateTo(ret);
					}
					else
					{
						IFuture<T> fut = agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(sid, propertyname);
						fut.delegateTo(ret);
					}
				}
			});
		}

		return ret;
	}
}
