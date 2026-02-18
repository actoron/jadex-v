package jadex.injection.impl;

import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import jadex.common.SUtil;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponent;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.InjectionModel.MDynVal;

/**
 *  Helper class for dynamic values for property change listener management.
 */
public class SPropertyChange
{
	/**
	 *  Add/remove the property change listener.
	 *  @param newbean	The new value, if any. The listener is added to it, if value is bean.
	 *  @param oldbean	The old value, if any. The listener is removed from it, if value is bean.
	 *  @param listener	The (cached) property change listener to add/remove.
	 *  @param comp	The component.
	 *  @param mdynval	The model element of the dynamic value.
	 *  @param source	The source object for the event (if null, the bean is used). No property name is given when source!=null.
	 *  				This enables also observing beans in collections/maps.
	 *  @return	The (new) property change listener to be cached for subsequent calls.
	 */
	public static <T> PropertyChangeListener	updateListener(T newbean, T oldbean, PropertyChangeListener listener, IComponent comp, MDynVal mdynval, Object source)
	{
		// Remove  bean listener from old value, if any.
		if(oldbean!=null && listener!=null)
		{
			try
			{
				MethodHandle	remover	= getRemover(oldbean.getClass());
				if(remover!=null)
				{
					remover.invoke(oldbean, listener);
				}
				
				// Listener added, but no remove method found.
				else if(getAdder(oldbean.getClass())!=null)
				{
					System.getLogger(SPropertyChange.class.getName())
						.log(System.Logger.Level.WARNING, "No removePropertyChangeListener() in: " + oldbean.getClass().getName()+". May lead to outdated events being processed.");
				}
			}
			catch(Throwable e)
			{
				// Shouldn't happen!?
				SUtil.throwUnchecked(e);
			}
		}
		
		// Add bean listener to new value
		if(newbean!=null)
		{
			try
			{
				MethodHandle	adder	= SPropertyChange.getAdder(newbean.getClass());
				if(adder!=null)
				{
					if(listener==null)
					{
						listener	= event ->
						{
							// When observing collection/map, use source as changed value. 
							Object	newvalue	= source!=null ? source : event.getSource();
							Object info	= source!=null ? null : event.getPropertyName();
							
							((InjectionFeature)comp.getFeature(IInjectionFeature.class))
								.valueChanged(new ChangeEvent(Type.CHANGED, mdynval.name(), newvalue, null, info), mdynval.field().getAnnotations());
						};
					}
					adder.invoke(newbean, listener);
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
