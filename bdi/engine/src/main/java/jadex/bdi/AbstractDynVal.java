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
	/**
	 *  The observation mode for inner values.
	 */
	public enum ObservationMode
	{
		/** Do not publish change events. */
		NONE,
		
		/** Publish events only when replacing the whole stored object. */
		VALUE,
		
		/** When the stored object is a collection/map, publish add/remove/change events. */
		COLLECTION,
		
		/** Default.
		 *  When the stored object is a bean, publish property change events.
		 *  Also publishes add/remove/change events when the stored object is a collection/map.
		 *  When a collection/map contains beans as values, property changes are published as generic change of the whole value. */
		COLLECTION_AND_BEAN
	}
	
	
	//-------- fields set during runtime --------
	
	/** The last value. */
	T	value;
	
	//-------- fields only set on init --------
	
	/** The component. */
	IComponent	comp;
	
	/** The change handler gets called after any change with old and new value. */
	IEventPublisher	changehandler;
	
	/** Observe changes of inner values (e.g. collections or beans). */
	ObservationMode	mode	= ObservationMode.COLLECTION_AND_BEAN;
	
	/** The (cached) property change listener for the value, if bean.
	 *  Created on first use. */
	PropertyChangeListener	listener;
	
	
	/**
	 *  Called on component init.
	 */
	void	init(IComponent comp, IEventPublisher changehandler)
	{
		this.comp	= comp;
		this.changehandler	= changehandler;
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
			
			if(mode!=ObservationMode.NONE && !SUtil.equals(old, value))
			{
				changehandler.entryChanged(comp, old, value, null);
			}
		}
	}
	
	/**
	 *  Set the observation mode for inner values.
	 *  Default is COLLECTION_AND_BEAN.
	 */
	public void setObservationMode(ObservationMode mode)
	{
		if(this.mode!=mode)
		{
			if(changehandler!=null)
			{
				if(this.mode==ObservationMode.COLLECTION_AND_BEAN)
				{
					// Remove old listener
					observeNewValue(value, null);
				}
				
				else if(mode==ObservationMode.COLLECTION_AND_BEAN)
				{
					// Add new listener
					observeNewValue(null, value);
				}
			}
			this.mode	= mode;
		}
	}

	/**
	 *  Observe inner value changes, i.e. collections or beans.
	 *  Adds/removes property change listeners and wraps collections.
	 */
	void observeNewValue(T old, T value) 
	{
		if(mode==ObservationMode.COLLECTION_AND_BEAN)
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
