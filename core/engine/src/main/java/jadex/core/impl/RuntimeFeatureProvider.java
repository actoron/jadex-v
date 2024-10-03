package jadex.core.impl;

import jadex.core.IComponentFeature;
import jadex.core.IRuntimeFeature;

public abstract class RuntimeFeatureProvider<T extends IRuntimeFeature> extends FeatureProvider<T>
{
	/**
	 *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
	 *  @param self	The component to which the feature belongs.
	 *  @return	The feature instance.
	 */
	public abstract <T> T createFeatureInstance();
}
