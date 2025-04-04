package jadex.injection.impl;

import java.util.List;

import jadex.core.IComponent;

/**
 *  Handler for performing an injection operation.
 */
@FunctionalInterface
public interface IInjectionHandle
{
	/**
	 *  Handle the injection at runtime, e.g. method invocation.
	 *  @param self	The component.
	 *  @param pojos	The actual pojo objects as a hierachy of component pojo plus subobjects, if any.
	 *  				The injection is for the last pojo in the list.
	 *  @param context	E.g. found service, BDI goal/plan...
	 */
	public void	handleInjection(IComponent self, List<Object> pojos, Object context);
}
