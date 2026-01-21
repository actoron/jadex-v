package jadex.injection.impl;

import java.util.Set;

import jadex.core.IComponent;
import jadex.injection.AbstractDynVal.ObservationMode;
import jadex.injection.impl.InjectionModel.MDynVal;

/**
 *  Transform changes to change events.
 */
public class SetWrapper<T> extends jadex.collection.SetWrapper<T>
{
	/** The wrapper helper for handling events and bean listening. */
	WrapperHelper<T>	helper;
	
	/**
	 *  Create a wrapper.
	 *  @param comp The component.
	 *  @param mdynval The model element of the dynamic value.
	 *  @param delegate The delegate map.
	 */
	public SetWrapper(IComponent comp, MDynVal mdynval, ObservationMode mode, Set<T> delegate)
	{
		super(delegate);
		this.helper	= new WrapperHelper<T>(comp, mdynval, mode, delegate);
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
