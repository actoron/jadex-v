package jadex.core.impl;

import java.util.Collections;
import java.util.Set;

/**
 *  Superclass for feature provider functionality for both Runtime and Component features.
 *  A feature providers is loaded from the classpath and adds a specific feature.
 */
public abstract class FeatureProvider<T>
{
	/**
	 *  Get the type of the feature used for accessing.
	 */
	public abstract Class<T> getFeatureType();
	
	/**
	 *  Determines if the feature is created immediately
	 *  on component startup (false) or later on first access (true).
	 *  @return	Defaults to false.
	 */
	public boolean isLazyFeature()
	{
		return false;
	}
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		return Collections.emptySet();
	}
}
