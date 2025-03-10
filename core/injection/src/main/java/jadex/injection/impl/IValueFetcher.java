package jadex.injection.impl;

import jadex.core.IComponent;

/**
 *  Handler for fetching the value for a field.
 */
public interface IValueFetcher
{
	/**
	 *  Handle the value extraction at runtime.
	 *  @param self	The component.
	 *  @param pojo	The actual pojo object (might be the component pojo or a subobject, e.g. service impl).
	 *  @param type	The type of the field/parameter to inject.
	 *  @param context	E.g. BDI goal/plan...
	 */
	public Object	getValue(IComponent self, Object pojo, Class<?> type, Object context);
}
