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
}
