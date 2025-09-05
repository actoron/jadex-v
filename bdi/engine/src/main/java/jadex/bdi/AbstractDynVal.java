package jadex.bdi;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;

import jadex.collection.CollectionWrapper;
import jadex.collection.IEventPublisher;
import jadex.collection.ListWrapper;
import jadex.collection.MapWrapper;
import jadex.collection.SPropertyChange;
import jadex.collection.SetWrapper;
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
		OFF,
		
		/** Publish events only when replacing the whole stored object. */
		ON_SET_VALUE,
		
		/** When the stored object is a collection/map, publish add/remove/change events. */
		ON_COLLECTION_CHANGE,
		
		/** When the stored object is a bean, publish property change events. */
		ON_BEAN_CHANGE,
		
		/** Default.
		 *  When the stored object is a bean, publish property change events.
		 *  Also publishes add/remove/change events when the stored object is a collection/map.
		 *  When a collection/map contains beans as values, property changes are published as generic change of the whole value. */
		ON_ALL_CHANGES,
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
	ObservationMode	mode	= ObservationMode.ON_ALL_CHANGES;
	
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
			
			if(mode!=ObservationMode.OFF && !SUtil.equals(old, value))
			{
				changehandler.entryChanged(comp, old, value, null);
			}
		}
	}
	
	/**
	 *  Set the observation mode for inner values.
	 *  Default is COLLECTION_AND_BEAN.
	 */
	public AbstractDynVal<T> setObservationMode(ObservationMode mode)
	{
		if(this.mode!=mode)
		{
			if(changehandler!=null)
			{
				// Remove old listeners if any.
				observeNewValue(value, null);
				
				// Set new mode after removing old listener but before adding new listener
				// for proper functioning of observeNewValue().
				this.mode	= mode;
				
				// Add new listener if any.
				observeNewValue(null, value);
			}
			else
			{
				this.mode	= mode;
			}
		}
		return this;
	}

	/**
	 *  Observe inner value changes, i.e. collections or beans.
	 *  Adds/removes property change listeners and wraps collections.
	 */
	void observeNewValue(T old, T value) 
	{
		// Stop observing old collection
		if(old instanceof CollectionWrapper<?>)
		{
			((CollectionWrapper<?>)old).setEventPublisher(null);
		}
		// Stop observing old map
		else if(old instanceof MapWrapper<?,?>)
		{
			((MapWrapper<?,?>)old).setEventPublisher(null);
		}
		// Stop observing old bean, if any
		else
		{
			listener	= SPropertyChange.updateListener(old, null, listener, comp, changehandler);
		}
		
		// Start observing new list
		if((mode==ObservationMode.ON_COLLECTION_CHANGE || mode==ObservationMode.ON_ALL_CHANGES) && value instanceof List<?>)
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			T t	= (T)(new ListWrapper((List<?>)value, changehandler, comp, mode==ObservationMode.ON_ALL_CHANGES));
			value	= t;
			this.value	= value;
		}
		// Start observing new set
		else if((mode==ObservationMode.ON_COLLECTION_CHANGE || mode==ObservationMode.ON_ALL_CHANGES) && value instanceof Set<?>)
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			T t	= (T)(new SetWrapper((Set<?>)value, changehandler, comp, mode==ObservationMode.ON_ALL_CHANGES));
			value	= t;
			this.value	= value;
		}
		// Start observing new map
		else if((mode==ObservationMode.ON_COLLECTION_CHANGE || mode==ObservationMode.ON_ALL_CHANGES) && value instanceof java.util.Map<?,?>)
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			T t	= (T)(new MapWrapper((java.util.Map<?,?>)value, changehandler, comp, mode==ObservationMode.ON_ALL_CHANGES));
			value	= t;
			this.value	= value;
		}
		// Start observing new bean, if any
		else if(mode==ObservationMode.ON_BEAN_CHANGE || mode==ObservationMode.ON_ALL_CHANGES)
		{
			listener	= SPropertyChange.updateListener(null, value, listener, comp, changehandler);
		}
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(get());
	}	
}
