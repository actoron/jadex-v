package jadex.injection.impl;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Map;

import jadex.common.SUtil;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponent;
import jadex.injection.AbstractDynVal.ObservationMode;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.InjectionModel.MDynVal;

/**
 *  Transform changes to change events.
 */
public class WrapperHelper<T>
{
	/** The component.*/
	IComponent	comp;
	
	/** The model element of the dynamic value. */
	MDynVal mdynval;
	
	/** The current observation mode. */
	ObservationMode	mode;
	
	/** The observed collection/map. */
	Object	source;
	
	/** The observed values. */
	Collection<T>	values;
	
	/** The (cached) property change listener for the values, if beans.
	 *  Created on first use. */
	PropertyChangeListener	listener;
	
	/**
	 *  Create a wrapper.
	 *  @param comp The component.
	 *  @param mdynval The fully qualified name of the dynamic value.
	 *  @param mode The observation mode.
	 *  @param delegate The observed map.
	 */
	public WrapperHelper(IComponent comp, MDynVal mdynval, ObservationMode mode, Map<?, T> delegate)
	{
		// Use the map values as entries to observe as beans.
		this(comp, mdynval, mode, delegate.values());
		// Use the map as source for events.
		this.source	= delegate;
	}
	
	/**
	 *  Create a wrapper.
	 *  @param comp The component.
	 *  @param mdynval The model element of the dynamic value.
	 *  @param mode The observation mode.
	 *  @param delegate The observed collection.
	 */
	public WrapperHelper(IComponent comp, MDynVal mdynval, ObservationMode mode, Collection<T> delegate)
	{
		this.comp	= comp;
		this.mdynval	= mdynval;
		this.mode	= mode;
		this.values	= delegate;
		this.source	= delegate;
	}
	
	public void setObservationMode(ObservationMode mode)
	{
		// Change from non-observing to observing.
		if(this.mode!=ObservationMode.ON_ALL_CHANGES && mode==ObservationMode.ON_ALL_CHANGES)
		{
			for(T entry: values)
			{
				// Register listener, if entry is bean.
				listener	= SPropertyChange.updateListener(entry, null, listener, comp, mdynval, source);
			}
		}
		
		// Change from observing to non-observing.
		if(this.mode==ObservationMode.ON_ALL_CHANGES && mode!=ObservationMode.ON_ALL_CHANGES)
		{
			for(T entry: values)
			{
				// Unregister listener, if entry is bean.
				listener	= SPropertyChange.updateListener(null, entry, listener, comp, mdynval, source);
			}
		}
		
		this.mode	= mode;
	}


	/**
	 *  An entry was added to the collection.
	 */
	protected void entryAdded(T value, Object info)
	{
		if(mode==ObservationMode.ON_ALL_CHANGES)
		{
			// Register listener, if entry is bean.
			listener	= SPropertyChange.updateListener(value, null, listener, comp, mdynval, source);
		}
		
		if(mode==ObservationMode.ON_ALL_CHANGES || mode==ObservationMode.ON_COLLECTION_CHANGE)
		{
			((InjectionFeature)comp.getFeature(IInjectionFeature.class))
				.valueChanged(new ChangeEvent(Type.ADDED, mdynval.name(), value, null, info), mdynval.field().getAnnotations());
		}

		//System.out.println("entry added: "+value+" to "+name+" mode="+mode);
	}

	/**
	 *  An entry was removed from the collection.
	 */
	protected void entryRemoved(T value, Object info)
	{
		if(mode==ObservationMode.ON_ALL_CHANGES)
		{
			// Unregister listener, if entry is bean.
			listener	= SPropertyChange.updateListener(null, value, listener, comp, mdynval, source);
		}
		
		if(mode==ObservationMode.ON_ALL_CHANGES || mode==ObservationMode.ON_COLLECTION_CHANGE)
		{
			((InjectionFeature)comp.getFeature(IInjectionFeature.class))
				.valueChanged(new ChangeEvent(Type.REMOVED, mdynval.name(), value, null, info), mdynval.field().getAnnotations());
		}
	}
	
	/**
	 *  An entry was changed in the collection.
	 */
	protected void entryChanged(T value, T oldvalue, Object info)
	{
		if(value!=oldvalue)
		{
			if(mode==ObservationMode.ON_ALL_CHANGES)
			{
				// (Un-)Register listener, if values are bean.
				listener	= SPropertyChange.updateListener(value, oldvalue, listener, comp, mdynval, source);
			}
			
			if(!SUtil.equals(value, oldvalue) && 
			   (mode==ObservationMode.ON_ALL_CHANGES || mode==ObservationMode.ON_COLLECTION_CHANGE))
			{
				((InjectionFeature)comp.getFeature(IInjectionFeature.class))
					.valueChanged(new ChangeEvent(Type.CHANGED, mdynval.name(), value, oldvalue, info), mdynval.field().getAnnotations());
			}
		}
	}
}
