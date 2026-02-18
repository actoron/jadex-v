package jadex.injection.impl;

import java.lang.annotation.Annotation;

import jadex.core.ChangeEvent;
import jadex.core.IComponent;

/**
 *  Generic change handler to map change events to engine-specific code.
 */
public interface IChangeHandler
{
	/**
	 *  Handle a change event.
	 *  @param comp		The component.
	 *  @param event	The change event.
	 *  @param annos	The annotations of the changed element, if any.
	 */
	public void handleChange(IComponent comp, ChangeEvent event, Annotation... annos);
}
