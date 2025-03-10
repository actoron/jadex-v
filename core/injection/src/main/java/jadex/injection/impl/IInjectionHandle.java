package jadex.injection.impl;

import jadex.core.IComponent;

/**
 *  Handler for performing an injection operation.
 */
@FunctionalInterface
public interface IInjectionHandle
{
	/**
	 *  Handle the injection at runtime.
	 *  @param self	The component.
	 *  @param pojo	The actual pojo object (might be the component pojo or a subobject, e.g. service impl).
	 *  @param context	E.g. BDI goal/plan...
	 */
	public void	handleInjection(IComponent self, Object pojo, Object context);
}
