package jadex.injection.impl;

import java.util.Map;

import jadex.core.IComponent;
import jadex.injection.AbstractDynVal.ObservationMode;

/**
 *  Transform changes to change events.
 */
public class MapWrapper<T, E> extends jadex.collection.MapWrapper<T, E>
{
	/** The wrapper helper for handling events and bean listening. */
	WrapperHelper<E>	helper;
	
	/**
	 *  Create a wrapper.
	 *  @param comp The component.
	 *  @param name The fully qualified name of the dynamic value.
	 *  @param delegate The delegate map.
	 */
	public MapWrapper(IComponent comp, String name, ObservationMode mode, Map<T, E> delegate)
	{
		super(delegate);
		this.helper	= new WrapperHelper<E>(comp, name, mode, delegate);
	}

	@Override
	protected void entryAdded(T key, E value)
	{
		helper.entryAdded(value, key);
	}

	@Override
	protected void entryRemoved(T key, E value)
	{
		helper.entryRemoved(value, key);
	}
	
	@Override
	protected void entryChanged(T key, E value, E oldvalue)
	{
		helper.entryChanged(value, oldvalue, key);
	}
	
	/**
	 * Set the observation mode.
	 * @param mode The new mode.
	 */
	public void setObservationMode(ObservationMode mode)
	{
		helper.setObservationMode(mode);
	}
}
