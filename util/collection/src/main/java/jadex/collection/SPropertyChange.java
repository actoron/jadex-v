package jadex.collection;

import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import jadex.common.SUtil;

/**
 *  Helper class for observable values for property change listener management.
 */
public class SPropertyChange
{
	/**
	 *  Add/remove the property change listener.
	 *  @param old	The old value, if any.
	 *  @param value	The new value, if any.
	 *  @param listener	The (cached) property change listener to add/remove.
	 *  @param context	The context for the publisher, e.g. the component.
	 *  @param publisher	The publisher (if any) gets called with entryChanged(): null -> new value.
	 *  @param source The source object for the event (if null, the value is used). No property name is given when source!=null.
	 *  @return	The (new) property change listener to be cached for subsequent calls.
	 */
	public static <T> PropertyChangeListener	updateListener(T old, T value, PropertyChangeListener listener, Object context, IEventPublisher publisher, Object source)
	{
		// Remove  bean listener from old value, if any.
		if(old!=null && listener!=null)
		{
			try
			{
				MethodHandle	remover	= getRemover(old.getClass());
				if(remover!=null)
				{
					remover.invoke(old, listener);
				}
				
				// Listener added, but no remove method found.
				else if(getAdder(old.getClass())!=null)
				{
					System.getLogger(SPropertyChange.class.getName())
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
		if(value!=null && publisher!=null)
		{
			try
			{
				MethodHandle	adder	= SPropertyChange.getAdder(value.getClass());
				if(adder!=null)
				{
					if(listener==null)
					{
						listener	= event ->
						{
							// Use event source to get new value even if listener is reused.
							publisher.entryChanged(context, null, source!=null ? source : event.getSource(), source!=null ? null : event.getPropertyName());
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
