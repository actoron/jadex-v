package jadex.bdi;

import java.beans.PropertyChangeListener;

import jadex.collection.IEventPublisher;
import jadex.collection.SPropertyChange;
import jadex.common.SUtil;
import jadex.core.IComponent;

/**
 *  Wrapper for observable values.
 *  Generates appropriate events on changes.
 */
public class Val<T>
{
	//-------- user fields --------
	
	/** The current value. */
	T	value;
	
	/** The property change listener for the value, if bean. */
	PropertyChangeListener	listener;
	
	//-------- fields set on init --------
	
	/** The component. */
	IComponent	comp;
	
	/** The change handler gets called after any change with old and new value. */
	IEventPublisher	changehandler;
	
	/** Observe changes of inner values (e.g. collections or beans). */
	boolean	observeinner;
	
	/**
	 *  Create an observable with a given value.
	 */
	public Val(T value)
	{
		doSet(value);
	}
	
	/**
	 *  Called on component init.
	 */
	void	init(IComponent comp, IEventPublisher changehandler, boolean observeinner)
	{
		this.comp	= comp;
		this.changehandler	= changehandler;
		this.observeinner	= observeinner;
		
		if(observeinner)
		{
			this.listener	= SPropertyChange.updateListener(null, value, listener, comp, changehandler);
		}
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
		
		if(changehandler!=null)
		{
			if(observeinner && old!=value)
			{
				listener	= SPropertyChange.updateListener(old, value, listener, comp, changehandler);
			}
			
			if(!SUtil.equals(old, value))
			{
				changehandler.entryChanged(comp, old, value, null);
			}
		}
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(get());
	}
}
