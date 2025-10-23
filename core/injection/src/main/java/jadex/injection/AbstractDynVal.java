package jadex.injection;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

import jadex.common.SUtil;
import jadex.core.ChangeEvent;
import jadex.core.IComponent;
import jadex.injection.impl.InjectionFeature;
import jadex.injection.impl.ListWrapper;
import jadex.injection.impl.MapWrapper;
import jadex.injection.impl.SPropertyChange;
import jadex.injection.impl.SetWrapper;

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
		ON_ALL_CHANGES;
		
		/**
		 * Check if a single bean value should be observed.
		 */
		boolean	isObserveBean()
		{
			return this==ON_BEAN_CHANGE || this==ON_ALL_CHANGES;
		}
	}
	
	
	//-------- fields set during runtime --------
	
	/** The last value. */
	T	value;
	
	/** Observe changes of inner values (e.g. collections or beans). */
	ObservationMode	mode	= ObservationMode.ON_ALL_CHANGES;
	
	/** The (cached) property change listener for the value, if bean.
	 *  Created on first use. */
	PropertyChangeListener	listener;
	
	//-------- fields set on init --------
	
	/** The component. */
	IComponent	comp;
	
	/** The fully qualified name of the value. */
	String	name;
	
	
	/**
	 *  Called on component init.
	 */
	void	init(IComponent comp, String name)
	{
		this.comp	= comp;
		this.name	= name;
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
		
		if(comp!=null)
		{
			if(old!=value)
			{
				observeNewValue(old, value);
			}
			
			if(mode!=ObservationMode.OFF && !SUtil.equals(old, value))
			{
				((InjectionFeature)comp.getFeature(IInjectionFeature.class))
					.valueChanged(new ChangeEvent(ChangeEvent.Type.CHANGED, name, value, old, null));
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
			if(value!=null && comp!=null)
			{
				if(value instanceof ListWrapper<?>)
				{
					((ListWrapper<?>)value).setObservationMode(mode);
				}
				else if(value instanceof SetWrapper<?>)
				{
					((SetWrapper<?>)value).setObservationMode(mode);
				}
				else if(value instanceof MapWrapper<?,?>)
				{
					((MapWrapper<?,?>)value).setObservationMode(mode);
				}
				else
				{
					// Change from non-observing to observing.
					if(!this.mode.isObserveBean() && mode.isObserveBean())
					{
						// Register listener, if entry is bean.
						listener	= SPropertyChange.updateListener(value, null, listener, comp, name, null);
					}
					
					// Change from observing to non-observing.
					else if(this.mode.isObserveBean() && !mode.isObserveBean())
					{
						// Unregister listener, if entry is bean.
						listener	= SPropertyChange.updateListener(null, value, listener, comp, name, null);
					}
				}
			}

			this.mode	= mode;
		}
		
		return this;
	}

	/**
	 *  Observe inner value changes, i.e. collections or beans.
	 *  Adds/removes property change listeners and wraps collections.
	 */
	void observeNewValue(T old, T value) 
	{
		// Stop observing old list
		if(old instanceof ListWrapper<?>)
		{
			((ListWrapper<?>)old).setObservationMode(ObservationMode.OFF);
		}
		
		// Stop observing old SET
		else if(old instanceof SetWrapper<?>)
		{
			((SetWrapper<?>)old).setObservationMode(ObservationMode.OFF);
		}
		
		// Stop observing old map
		else if(old instanceof MapWrapper<?,?>)
		{
			((MapWrapper<?,?>)old).setObservationMode(ObservationMode.OFF);
		}
		
		// Stop observing old bean, if any
		else
		{
			listener	= SPropertyChange.updateListener(null, old, listener, comp, name, null);
		}
		
		
		// Start observing new list
		if(value instanceof ListWrapper<?>)
		{
			throw new IllegalStateException("Value is already wrapped: "+value);
		}
		else if(value instanceof List<?>)
		{
			// Wrap the list.
			@SuppressWarnings({ "unchecked", "rawtypes" })
			T t	= (T)(new ListWrapper(comp, name, mode, (List<?>)value));
			value	= t;
			this.value	= value;
		}
		
		// Start observing new set
		else if(value instanceof SetWrapper<?>)
		{
			throw new IllegalStateException("Value is already wrapped: "+value);
		}
		else if(value instanceof List<?>)
		{
			// Wrap the list.
			@SuppressWarnings({ "unchecked", "rawtypes" })
			T t	= (T)(new ListWrapper(comp, name, mode, (List<?>)value));
			value	= t;
			this.value	= value;
		}
		
		// Start observing new map
		else if(value instanceof MapWrapper<?, ?>)
		{
			throw new IllegalStateException("Value is already wrapped: "+value);
		}
		else if(value instanceof Map<?, ?>)
		{
			// Wrap the list.
			@SuppressWarnings({ "unchecked", "rawtypes" })
			T t	= (T)(new MapWrapper(comp, name, mode, (Map<?, ?>)value));
			value	= t;
			this.value	= value;
		}
		
		// Start observing new bean, if any
		else if(mode==ObservationMode.ON_BEAN_CHANGE || mode==ObservationMode.ON_ALL_CHANGES)
		{
			listener	= SPropertyChange.updateListener(value, null, listener, comp, name, null);
		}
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(get());
	}	
}
