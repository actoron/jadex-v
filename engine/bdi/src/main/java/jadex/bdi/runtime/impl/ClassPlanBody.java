package jadex.bdi.runtime.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jadex.bdi.annotation.PlanAPI;
import jadex.bdi.annotation.PlanCapability;
import jadex.bdi.annotation.PlanReason;
import jadex.bdi.model.MBody;
import jadex.bdi.model.MElement;
import jadex.bdi.model.MPlan;
import jadex.bdi.runtime.ChangeEvent;
import jadex.bdi.runtime.ICapability;
import jadex.bdi.runtime.PlanFailureException;
import jadex.common.MethodInfo;
import jadex.common.SAccess;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.micro.MicroAgent;
import jadex.rules.eca.ChangeInfo;

/**
 *  Plan body that is represented as complete class.
 */
public class ClassPlanBody extends AbstractPlanBody
{
	//-------- attributes --------
	
	/** The body class. */
	protected Class<?> body;
	
	/** The body instance. */
	protected Object plan;
	
	
	/** The body method. */
	protected Method bodymethod;
	
	/** The passed method. */
	protected Method passedmethod;

	/** The failed method. */
	protected Method failedmethod;

	/** The aborted method. */
	protected Method abortedmethod;
	
	//--------- constructors ---------
	
	/**
	 *  Create a new plan body.
	 */
	public ClassPlanBody(RPlan rplan, Class<?> body)
	{
		this(rplan, body, null);
	}
	
	/**
	 *  Create a new plan body.
	 */
	public ClassPlanBody(RPlan rplan, Object plan)
	{
		this(rplan, plan.getClass(), plan);
	}
	
	/**
	 *  Create a new plan body.
	 */
	public ClassPlanBody(RPlan rplan, Class<?> body, Object plan)
	{
		super(rplan);
		this.body = body;
		this.plan = plan;
		ClassLoader	cl	= IInternalBDIAgentFeature.get().getClassLoader();
//		Class<?> mbd = body!=null? body: plan.getClass();
		MBody mbody = ((MPlan)rplan.getModelElement()).getBody();
		bodymethod = mbody.getBodyMethod(cl).getMethod(cl);
		MethodInfo mi = mbody.getPassedMethod(cl);
		if(mi!=null)
			passedmethod = mi.getMethod(cl);
		mi = mbody.getFailedMethod(cl);
		if(mi!=null)
			failedmethod = mi.getMethod(cl);
		mi = mbody.getAbortedMethod(cl);
		if(mi!=null)
			abortedmethod = mi.getMethod(cl);
		
		if(plan!=null)
			injectElements();//ia.getComponentFeature(IPojoComponentFeature.class).getPojoAgent());
	}
	
	//-------- methods --------
	
	/**
	 *  Get or create the body.
	 */
	public Object getBody()
	{
		if(plan==null)
		{
			try
			{
				// create plan  
//				if(plan==null)
//				{
					Constructor<?>[] cons = body.getDeclaredConstructors();
					for(Constructor<?> c: cons)
					{
						Object[] params = BDIAgentFeature
							.getInjectionValues(c.getParameterTypes(), c.getParameterAnnotations(), rplan.getModelElement(), null, rplan, null);
						if(params!=null)
						{
							try
							{
								SAccess.setAccessible(c, true);
								plan = c.newInstance(params);
								break;
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}						
					}
					if(plan==null)
						throw new RuntimeException("Plan body has no accessible constructor (maybe wrong args?): "+body);
//				}
				
				injectElements();
			}
			catch(RuntimeException e)
			{
				StringWriter	sw	= new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				System.err.println("Plan '"+this+"' threw exception: "+sw);
				throw e;
			}
			catch(Exception e)
			{
				StringWriter	sw	= new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				System.err.println("Plan '"+this+"' threw exception: "+sw);
				throw new RuntimeException(e);
			}
		}
		
		return plan;
	}

	/**
	 *  Get the plan.
	 *  @return The plan.
	 */
	public Object getPojoPlan()
	{
		return plan;
	}

	/**
	 *  Inject plan elements.
	 */
	protected void injectElements()
	{
		try
		{
			Class<?> bcl = body;
			while(!Object.class.equals(bcl))
			{
				Field[] fields = bcl.getDeclaredFields();
				for(Field f: fields)
				{
					if(f.isAnnotationPresent(PlanAPI.class))
					{
						SAccess.setAccessible(f, true);
						f.set(plan, getRPlan());
					}
					else if(f.isAnnotationPresent(PlanCapability.class))
					{
						// Find capability based on model element (or use agent).
						String	capaname	= null;
						int idx = rplan.getModelElement().getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR);
						if(idx!=-1)
						{
							capaname = rplan.getModelElement().getName().substring(0, idx);
						}

						// Pojo specific code.
						Object pojocapa	= capaname!=null
							? IInternalBDIAgentFeature.get().getCapabilityObject(capaname)
							: ((MicroAgent)getAgent()).getPojo();

						
						if(f.getType().isAssignableFrom(IComponent.class))
						{
							SAccess.setAccessible(f, true);
							f.set(plan, new CapabilityPojoWrapper(pojocapa, capaname).getAgent());
						}
						else if(f.getType().isAssignableFrom(ICapability.class))
						{
							SAccess.setAccessible(f, true);
							f.set(plan, new CapabilityPojoWrapper(pojocapa, capaname));
						}
						else if(pojocapa!=null && f.getType().isAssignableFrom(pojocapa.getClass()))
						{
							SAccess.setAccessible(f, true);
							f.set(plan, pojocapa);
						}
						else
						{
							throw new RuntimeException("Cannot set @PlanCapability: "+f+", capaname="+capaname+", pojocapa="+pojocapa);
						}
					}
					else if(f.isAnnotationPresent(PlanReason.class))
					{
						Object r = getRPlan().getReason();
						if(r instanceof RProcessableElement)
						{
							Object reason = ((RProcessableElement)r).getPojoElement();
							if(reason!=null)
							{
								SAccess.setAccessible(f, true);
								f.set(plan, reason);
							}
							else 
							{
								SAccess.setAccessible(f, true);
								f.set(plan, r);
							}
						}
						else if(r instanceof ChangeEvent)
						{
							Class<?> ft = f.getType();
							SAccess.setAccessible(f, true);
							if(ft.equals(ChangeEvent.class))
							{
								f.set(plan, r);
							}
							else
							{
								Object val = ((ChangeEvent)r).getValue();
								if(val instanceof ChangeInfo)
								{
									if(ft.equals(ChangeInfo.class))
									{
										f.set(plan, val);
									}
									else
									{
										f.set(plan, ((ChangeInfo<?>)val).getValue());
									}
								}
								else
								{
									f.set(plan, val);
								}
							}
						}
					}
				}
				
				bcl = bcl.getSuperclass();
			}
		}
		catch(RuntimeException e)
		{
			throw e;
		}
		catch(Exception e)
		{
//			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	/**
	 *  Invoke the body.
	 */
	public Object invokeBody(Object[] params)
	{
		try
		{
			getBody();
			SAccess.setAccessible(bodymethod, true);
			return bodymethod.invoke(plan, params);
		}
		catch(Throwable t)
		{
			t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
			if(t instanceof NoClassDefFoundError)
			{
				throw new PlanFailureException("Could not create plan "+getRPlan(), t);
			}
			else
			{
				throw SUtil.throwUnchecked(t);
			}
		}
	}

	/**
	 *  Invoke the plan passed method.
	 */
	public Object invokePassed(Object[] params)
	{
		Object ret = null;
		if(passedmethod!=null)
		{
			try
			{
				SAccess.setAccessible(passedmethod, true);
				ret = passedmethod.invoke(plan, params);			
			}
			catch(Throwable t)
			{
				t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
				if(t instanceof Error)
				{
					throw (Error)t;
				}
				else if(t instanceof RuntimeException)
				{
					throw (RuntimeException)t;
				}
				else
				{
					throw new RuntimeException(t);
				}
			}
		}
		return ret;
	}

	/**
	 *  Invoke the plan failed method.
	 */
	public Object invokeFailed(Object[] params)
	{
		Object ret = null;
		if(failedmethod!=null)
		{
			try
			{
				SAccess.setAccessible(failedmethod, true);
				ret = failedmethod.invoke(plan, params);			
			}
			catch(Throwable t)
			{
				t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
				if(t instanceof Error)
				{
					throw (Error)t;
				}
				else if(t instanceof RuntimeException)
				{
					throw (RuntimeException)t;
				}
				else
				{
					throw new RuntimeException(t);
				}
			}
		}
		return ret;
	}

	/**
	 *  Invoke the plan aborted method.
	 */
	public Object invokeAborted(Object[] params)
	{
		Object ret = null;
		if(abortedmethod!=null)
		{
			try
			{
				SAccess.setAccessible(abortedmethod, true);
				ret = abortedmethod.invoke(plan, params);			
			}
			catch(Throwable t)
			{
				t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
				if(t instanceof Error)
				{
					throw (Error)t;
				}
				else if(t instanceof RuntimeException)
				{
					throw (RuntimeException)t;
				}
				else
				{
					throw new RuntimeException(t);
				}
			}
		}
		return ret;
	}
	
	/**
	 *  Get the passed parameters.
	 */
	public Class<?>[] getPassedParameterTypes()
	{
		return passedmethod==null? null: passedmethod.getParameterTypes();
	}

	/**
	 *  Get the failed parameters.
	 */
	public Class<?>[] getFailedParameterTypes()
	{
		return failedmethod==null? null: failedmethod.getParameterTypes();
		
	}

	/**
	 *  Get the aborted parameters.
	 */
	public Class<?>[] getAbortedParameterTypes()
	{
		return abortedmethod==null? null: abortedmethod.getParameterTypes();		
	}
	
	/**
	 *  Get the body parameter types.
	 */
	public Class<?>[] getBodyParameterTypes()
	{
		return bodymethod.getParameterTypes();
	}
}