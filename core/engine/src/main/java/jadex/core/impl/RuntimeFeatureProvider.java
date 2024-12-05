package jadex.core.impl;

import java.util.Collections;
import java.util.Set;

import jadex.core.IRuntimeFeature;

public abstract class RuntimeFeatureProvider<T extends IRuntimeFeature> extends FeatureProvider<T>
{
	/**
	 *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
	 *  @param self	The component to which the feature belongs.
	 *  @return	The feature instance.
	 */
	public abstract T createFeatureInstance();
	
	/**
	 *  Get the feature dependencies, i.e. features that are required to be available
	 *  before this one can be requested.
	 *  @return The dependencies.
	 */
	public Set<Class<? extends IRuntimeFeature>> getDependencies()
	{
		return Collections.emptySet();
	}
}
