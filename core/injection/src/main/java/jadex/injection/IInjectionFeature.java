package jadex.injection;

import jadex.core.IComponentFeature;

/**
 *  Marker interface for injection feature allowing e.g. @OnStart methods and field injections.
 */
public interface IInjectionFeature extends IComponentFeature
{
	/**
	 *  Add a result.
	 *  Also notifies result subscribers, if any.
	 */
	public void addResult(String name, Object value);

}
