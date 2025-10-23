package jadex.injection.impl;

import java.util.List;

import jadex.core.IComponent;
import jadex.injection.AbstractDynVal.ObservationMode;

/**
 *  Transform changes to change events.
 */
public class ListWrapper<T> extends jadex.collection.ListWrapper<T>
{
	/** The wrapper helper for handling events and bean listening. */
	WrapperHelper<T>	helper;
	
	/**
	 *  Create a wrapper.
	 *  @param comp The component.
	 *  @param name The fully qualified name of the dynamic value.
	 *  @param delegate The delegate map.
	 */
	public ListWrapper(IComponent comp, String name, ObservationMode mode, List<T> delegate)
	{
		super(delegate);
		this.helper	= new WrapperHelper<T>(comp, name, mode, delegate);
	}
	
	@Override
	protected void entryAdded(T value, Integer index)
	{
		helper.entryAdded(value, index);
	}
	
	@Override
	protected void entryRemoved(T value, Integer index)
	{
		helper.entryRemoved(value, index);
	}

	@Override
	protected void entryChanged(T value, T oldvalue, Integer index)
	{
		helper.entryChanged(value, oldvalue, index);
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
