package jadex.autostart;

import jadex.core.impl.RuntimeFeatureProvider;

public class AutostartFeatureProvider extends RuntimeFeatureProvider<IAutostartFeature>
{
	/**
	 *  Get the type of the feature used for accessing.
	 */
	public Class<IAutostartFeature> getFeatureType()
	{
		return IAutostartFeature.class;
	}
	
	/**
	 *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
	 *  @param self	The component to which the feature belongs.
	 *  @return	The feature instance.
	 */
	public IAutostartFeature createFeatureInstance()
	{
		return new AutostartFeature();
	}
	
	/**
	 *  Determines if the feature is created immediately
	 *  on component startup (false) or later on first access (true).
	 *  @return	Defaults to false.
	 */
	public boolean isLazyFeature()
	{
		return false;
	}
}
