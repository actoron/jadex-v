package jadex.injection.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import jadex.collection.IEventPublisher;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.injection.Dyn;
import jadex.injection.Val;

/**
 *  Helper class for observable values.
 *  Provides common methods and access to non-user methods.
 */
public class DynValHelper
{
	//-------- Dyn-initialization --------
	
	/** Protected init method. */
	static MethodHandle	dyn_init;
	static
	{
		try
		{
			Method	m	= Dyn.class.getDeclaredMethod("init", IComponent.class, IEventPublisher.class);
			m.setAccessible(true);
			dyn_init	= MethodHandles.lookup().unreflect(m);
		}
		catch(Exception e)
		{
			SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Call protected init method of Dyn.
	 */
	protected static void	initDyn(Dyn<Object> dyn, IComponent comp, IEventPublisher changehandler)
	{
		try
		{
			dyn_init.invoke(dyn, comp, changehandler);
		}
		catch(Throwable t)
		{
			SUtil.throwUnchecked(t);
		}
	}
	
	//-------- Val-initialization --------
	
	/** Protected init method. */
	static MethodHandle	val_init;
	static
	{
		try
		{
			Method	m	= Val.class.getDeclaredMethod("init", IComponent.class, IEventPublisher.class);
			m.setAccessible(true);
			val_init	= MethodHandles.lookup().unreflect(m);
		}
		catch(Exception e)
		{
			SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Call protected init method of Val.
	 */
	protected static void	initVal(Val<Object> val, IComponent comp, IEventPublisher changehandler)
	{
		try
		{
			val_init.invoke(val, comp, changehandler);
		}
		catch(Throwable t)
		{
			SUtil.throwUnchecked(t);
		}
	}
}
