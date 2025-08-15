package jadex.bdi.impl;

import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import jadex.bdi.Dyn;
import jadex.bdi.Val;
import jadex.common.SUtil;
import jadex.core.IComponent;

/**
 *  Helper class for observable values.
 *  Provides common methods and access to non-user methods.
 */
public class DynValHelper
{
	//-------- helper methods for Dyn/Val objects --------
	
	/**
	 *  Add/remove the property change listener.
	 *  @param value	The new value.
	 *  @param old	The old value.
	 *  @param listener	The property change listener to add/remove.
	 *  @param changehandler	The change handler gets called after any change with old and new value.
	 *  @return	The (new) property change listener.
	 */
	public static <T> PropertyChangeListener	updateListener(T value, T old, PropertyChangeListener listener, BiConsumer<T, T> changehandler)
	{
		// Remove  bean listener from old value, if any.
		if(old!=null && listener!=null)
		{
			try
			{
				MethodHandle	remover	= DynValHelper.getRemover(old.getClass());
				if(remover!=null)
				{
					remover.invoke(old, listener);
				}
				else
				{
					System.getLogger(DynValHelper.class.getName())
						.log(System.Logger.Level.WARNING, "No removePropertyChangeListener() in: " + old.getClass().getName()+". May lead to outdated events being processed.");
				}
			}
			catch(Throwable e)
			{
				// Shouldn't happen!?
				SUtil.throwUnchecked(e);
			}
		}
		
		// Add bean listener to new value
		if(value!=null)
		{
			try
			{
				MethodHandle	adder	= DynValHelper.getAdder(value.getClass());
				if(adder!=null)
				{
					if(listener==null)
					{
						listener	= event ->
						{
							if(changehandler!=null)
							{
								// Use evnt source to get new value even if listener is reused.
								@SuppressWarnings("unchecked")
								T source	= (T)event.getSource();
								changehandler.accept(null, source);
							}
						};
					}
					adder.invoke(value, listener);
				}
			}
			catch(Throwable e)
			{
				// Shouldn't happen!?
				SUtil.throwUnchecked(e);
			}
		}
		
		return listener;
	}

	//-------- Dyn-initialization --------
	
	/** Protected init method. */
	static MethodHandle	dyn_init;
	static
	{
		try
		{
			Method	m	= Dyn.class.getDeclaredMethod("init", IComponent.class, BiConsumer.class);
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
	protected static void	initDyn(Dyn<Object> dyn, IComponent comp, BiConsumer<Object, Object> changehandler)
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
			Method	m	= Val.class.getDeclaredMethod("init", BiConsumer.class);
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
	protected static void	initVal(Val<Object> val, BiConsumer<Object, Object> changehandler)
	{
		try
		{
			val_init.invoke(val, changehandler);
		}
		catch(Throwable t)
		{
			SUtil.throwUnchecked(t);
		}
	}
	
	//-------- property change support --------
	
	/** Method handles to add change listener. */
	static Map<Class<?>, MethodHandle>	adders	= new LinkedHashMap<>();
	
	/** Method handles to remove change listener. */
	static Map<Class<?>, MethodHandle>	removers	= new LinkedHashMap<>();
	
	/**
	 *  Get method handle to add change listener or null, if method not present.
	 */
	public static MethodHandle	getAdder(Class<?> clazz)
	{
		synchronized(adders)
		{
			if(adders.containsKey(clazz))
			{
				return adders.get(clazz);
			}
			else
			{
				try
				{
					Method	m	= clazz.getMethod("addPropertyChangeListener", PropertyChangeListener.class);
					MethodHandle	ret	= MethodHandles.lookup().unreflect(m);
					adders.put(clazz, ret);
					return ret;
				}
				catch(Exception e)
				{
					adders.put(clazz, null);
					return null;
				}
			}
		}
	}
	
	/**
	 *  Get method handle to remove change listener or null, if method not present.
	 */
	public static MethodHandle	getRemover(Class<?> clazz)
	{
		synchronized(removers)
		{
			if(removers.containsKey(clazz))
			{
				return removers.get(clazz);
			}
			else
			{
				try
				{
					Method	m	= clazz.getMethod("removePropertyChangeListener", PropertyChangeListener.class);
					MethodHandle	ret	= MethodHandles.lookup().unreflect(m);
					removers.put(clazz, ret);
					return ret;
				}
				catch(Exception e)
				{
					removers.put(clazz, null);
					return null;
				}
			}
		}
	}
}
