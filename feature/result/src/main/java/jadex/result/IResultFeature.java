package jadex.result;

import jadex.core.IComponentFeature;

/**
 *  Provide results of a component.
 */
public interface IResultFeature	extends IComponentFeature
{
	/**
	 *  Set a component result.
	 */
	public void	setResult(String name, Object value);
}
