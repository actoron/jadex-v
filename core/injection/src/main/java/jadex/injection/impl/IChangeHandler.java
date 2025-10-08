package jadex.injection.impl;

import jadex.core.ChangeEvent;
import jadex.core.IComponent;

/**
 *  Generic change handler to map change events to engine-specific code.
 */
public interface IChangeHandler
{
	/**
	 *  Handle a change event.
	 */
	public void handleChange(IComponent comp, ChangeEvent event);
}
