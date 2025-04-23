package jadex.bdi;

import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import jadex.common.SUtil;

/**
 *  Wrapper for belief values.
 *  Generates appropriate rule events on changes.
 */
public class Val<T>
{
	T	value;
	Callable<T>	dynamic;
	
	PropertyChangeListener	listener;
	
	// Fields set on init
	BiConsumer<T, T>	changehandler;
	boolean	updaterate;
	
	/**
	 *  Create belief value with a given value.
	 */
	public Val(T value)
	{
		doSet(value);
	}
	
	/**
	 *  Create belief value with a dynamic function.
	 */
	public Val(Callable<T> dynamic)
	{
		this.dynamic	= dynamic;
	}
	
	/**
	 *  Called on agent init.
	 *  @param changehandler	The change handler gets called after any change with old and new value.
	 *  @param updaterate	Flag to indicate that the value is externally updated.
	 */
	protected void	init(BiConsumer<T, T> changehandler, boolean updaterate)
	{
		this.changehandler	= changehandler;
		this.updaterate	= updaterate;
	}
	
	/**
	 *  Get the current value.
	 */
	public T get()
	{
		T ret = null;
		try
		{
			ret = dynamic!=null && !updaterate? dynamic.call(): value;
		}
		catch(Exception e)
		{
			//e.printStackTrace();
			throw SUtil.throwUnchecked(e);
		}
		
		//if(ret==null)
		//System.out.println("val get: "+ret);
	
		return ret;
	}

	/**
	 *  Set the value.
	 *  @throws IllegalStateException when dynamic is used.
	 */
	public void	set(T value)
	{
		if(dynamic!=null)
			throw new IllegalStateException("Should not set value on dynamic belief.");
		
		if(changehandler==null)
			throw new IllegalStateException("Wrapper not inited. Missing @Belief/@GoalParameter annotation?");
		
		doSet(value);
	}
	
	/**
	 *  Set the value and throw the event.
	 *  Used e.g. for update rate.
	 */
	protected void doSet(T value)
	{
		T	old	= this.value;
		this.value	= value;
		
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
				MethodHandle	adder	= getAdder(value.getClass());
				if(adder!=null)
				{
					if(listener==null)
					{
						listener	= event ->
						{
							if(changehandler!=null)
							{
								changehandler.accept(null, value);
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
		
		if(changehandler!=null)
		{
			changehandler.accept(old, value);
		}
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(get());
	}
	
	//-------- property change support --------
	
	/** Method handles to add change listener. */
	protected static Map<Class<?>, MethodHandle>	adders	= new LinkedHashMap<>();
	
	/** Method handles to remove change listener. */
	protected static Map<Class<?>, MethodHandle>	removers	= new LinkedHashMap<>();
	
	/**
	 *  Get method handle to add change listener or null, if method not present.
	 */
	protected MethodHandle	getAdder(Class<?> clazz)
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
	protected MethodHandle	getRemover(Class<?> clazz)
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
					Method	m	= clazz.getMethod("removePropertyChangeListener", PropertyChangeListener.class);
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
}
