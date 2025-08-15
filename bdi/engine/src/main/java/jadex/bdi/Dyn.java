package jadex.bdi;

import java.beans.PropertyChangeListener;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jadex.bdi.impl.DynValHelper;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;

/**
 *  Wrapper for a dynamic value.
 *  Generates appropriate events on changes.
 *  Supports dynamic values that are updated on every access or periodically.
 */
public class Dyn<T>
{
	/** The dynamic value provider. */
	final Callable<T>	dynamic;

	/** The update rate in millis, if > 0. */
	long	updaterate;
	
	/** Counter to be incremented whenever a new update rate is set. */
	int	modcount;

	/** The last value (when using update rate). */
	T	value;
	
	/** The property change listener for the value, if bean. */
	PropertyChangeListener	listener;
	
	//-------- fields set on init --------
	
	/** The component. */
	IComponent	comp;
	
	/** The change handler gets called after any change with old and new value. */
	BiConsumer<T, T>	changehandler;
	
	/**
	 *  Create an observable value with a dynamic function.
	 */
	public Dyn(Callable<T> dynamic)
	{
		this.dynamic	= dynamic;
	}
	
	/**
	 *  Set the update rate.
	 *  @param updaterate	The update rate in millis, or 0 for no periodic updates.
	 */
	public Dyn<T>	setUpdateRate(long updaterate)
	{
		this.updaterate	= updaterate;
		// Increment the modcount to "cancel" old timers.
		int fmodcount	= ++modcount;
		
		// When already inited -> start periodic updates.
		if(updaterate>0 && comp!=null)
		{
			IExecutionFeature	exe	= comp.getFeature(IExecutionFeature.class);
			Consumer<Void>	update	= new Consumer<Void>()
			{
				Consumer<Void>	update	= this;
				@Override
				public void accept(Void t)
				{
					// Only update if the update rate has not changed in the meantime.
					if(fmodcount==modcount)
					{
						try
						{
							doSet(dynamic.call());
							exe.waitForDelay(updaterate).then(update);
						}
						catch(Exception e)
						{
							SUtil.throwUnchecked(e);
						}
					}
				}
			};
			// Must happen after injections but before on start
			update.accept(null);
		}
		
		return this;
	}
	
	/**
	 *  Called on component init.
	 */
	void	init(IComponent comp, BiConsumer<T, T> changehandler)
	{
		this.comp	= comp;
		this.changehandler	= changehandler;
		
		// Set update rate to start periodic updates.
		setUpdateRate(updaterate);
	}
	
	/**
	 *  Get the current value.
	 */
	public T get()
	{
		T ret = null;
		try
		{
			ret = updaterate<=0 ? dynamic.call() : value;
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
		
		return ret;
	}
	
	/**
	 *  Set the value and throw the event.
	 *  Used e.g. for update rate.
	 */
	void doSet(T value)
	{
		T	old	= this.value;
		this.value	= value;
		
		listener	= DynValHelper.updateValue(value, old, listener, changehandler);
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(get());
	}	
}
