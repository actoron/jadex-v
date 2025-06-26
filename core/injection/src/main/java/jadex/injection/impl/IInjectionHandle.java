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
	 *  
	 *  @param self	The component.
	 *  @param pojos	The actual pojo objects as a hierachy of component pojo plus subobjects, if any.
	 *  				The injection is for the last pojo in the list.
	 *  @param context	E.g. found service, BDI goal/plan...
	 *  @param oldval	The previous value, e.g. for field injections.
	 *  
	 *  @return  A result when fetching a value for a field or performing a method invocation with non-void return type.
	 */
	public Object	apply(IComponent self, List<Object> pojos, Object context, Object oldval);
	
	/**
	 *  Returns true when the handle represents a static method call.
	 */
	public default boolean isStatic()
	{
		return false;
	}
}
