package jadex.injection.impl;

import java.util.List;

import jadex.core.IComponent;

/**
 *  Handler for fetching the value for a field.
 */
public interface IValueFetcher
{
	/**
	 *  Handle the value extraction at runtime.
	 *  @param self	The component.
	 *  @param pojos	The actual pojo objects as a hierachy of component pojo plus subobjects, if any.
	 *  				The injection is for the last pojo in the list.
	 *  @param context	E.g. BDI goal/plan...
	 */
	public Object	getValue(IComponent self, List<Object> pojos, Object context);
}
