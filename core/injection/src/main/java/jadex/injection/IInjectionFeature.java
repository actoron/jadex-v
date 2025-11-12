package jadex.injection;

import jadex.core.IChangeListener;
import jadex.core.IComponentFeature;

/**
 *  Marker interface for injection feature allowing e.g. @OnStart methods and field injections.
 */
public interface IInjectionFeature extends IComponentFeature
{
	/**
	 *  Set a result.
	 *  Also notifies result subscribers, if any.
	 */
	public void setResult(String name, Object value);
	
	/**
	 *  Add a change listener for a dynamic value.
	 *  @param name		The fully qualified name of the dynamic value.
	 */
	public void addListener(String name, IChangeListener listener);
	
	/**
	 *  Remove a change listener for a dynamic value.
	 *  @param name		The fully qualified name of the dynamic value.
	 */
	public void removeListener(String name, IChangeListener listener);
}
