package jadex.injection.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.injection.AbstractDynVal;

/**
 *  Helper class for accessing internal Dyn/Val methods.
 */
public class DynValHelper
{
	/** Protected init method. */
	static MethodHandle	init;
	static
	{
		try
		{
			Method	m	= AbstractDynVal.class.getDeclaredMethod("init", IComponent.class, String.class);
			m.setAccessible(true);
			init	= MethodHandles.lookup().unreflect(m);
		}
		catch(Exception e)
		{
			SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Call protected init method..
	 */
	protected static void	init(AbstractDynVal<Object> dynval, IComponent comp, String name)
	{
		try
		{
			init.invoke(dynval, comp, name);
		}
		catch(Throwable t)
		{
			SUtil.throwUnchecked(t);
		}
	}
}
