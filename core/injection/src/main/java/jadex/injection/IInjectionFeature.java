package jadex.injection;

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

}
