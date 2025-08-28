package jadex.bdi;

import java.beans.PropertyChangeListener;

import jadex.collection.IEventPublisher;
import jadex.collection.SPropertyChange;
import jadex.common.SUtil;
import jadex.core.IComponent;

/**
 *  Common base class for observable values.
 */
public class AbstractDynVal<T>
{
	/** The last value. */
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
	 *  Called on component init.
	 */
	void	init(IComponent comp, IEventPublisher changehandler, boolean observeinner)
	{
		this.comp	= comp;
		this.changehandler	= changehandler;
		this.observeinner	= observeinner;
	}
	
	/**
	 *  Get the current value.
	 *  Gets the dynamic value on every call when no update rate is set.
	 *  When an update rate is set, the last updated value is returned.
	 */
	public T get()
	{
		return value;
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
			if(old!=value)
			{
				observeNewValue(old, value);
			}
			
			if(!SUtil.equals(old, value))
			{
				changehandler.entryChanged(comp, old, value, null);
			}
		}
	}

	/**
	 *  Observe inner value changes, i.e. collections or beans.
	 *  Adds/removes property change listeners and wraps collections.
	 */
	void observeNewValue(T old, T value) 
	{
		if(observeinner)
		{
			listener	= SPropertyChange.updateListener(old, value, listener, comp, changehandler);
		}
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(get());
	}	
}
