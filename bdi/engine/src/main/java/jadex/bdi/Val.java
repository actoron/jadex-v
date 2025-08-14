package jadex.bdi;

import java.beans.PropertyChangeListener;
import java.util.function.BiConsumer;

import jadex.bdi.impl.DynValHelper;

/**
 *  Wrapper for belief values.
 *  Generates appropriate rule events on changes.
 */
public class Val<T>
{
	//-------- user fields --------
	
	/** The current value. */
	T	value;
	
	PropertyChangeListener	listener;
	
	//-------- fields set on init --------
	
	BiConsumer<T, T>	changehandler;
	
	/**
	 *  Create belief value with a given value.
	 */
	public Val(T value)
	{
		doSet(value);
	}
	
	/**
	 *  Called on agent init.
	 *  @param changehandler	The change handler gets called after any change with old and new value.
	 *  @param updaterate	Flag to indicate that the value is externally updated.
	 */
	void	init(BiConsumer<T, T> changehandler)
	{
		this.changehandler	= changehandler;
	}
	
	/**
	 *  Get the current value.
	 */
	public T get()
	{
		return value;
	}

	/**
	 *  Set the value.
	 *  @throws IllegalStateException when called before init.
	 */
	public void	set(T value)
	{
		if(changehandler==null)
			throw new IllegalStateException("Wrapper not inited. Missing @Belief/@GoalParameter annotation?");
		
		doSet(value);
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
