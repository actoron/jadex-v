package jadex.llm;

import jadex.core.impl.RuntimeFeatureProvider;

public class LlmFeatureProvider extends RuntimeFeatureProvider<ILlmFeature>
{
	/**
	 *  Get the type of the feature used for accessing.
	 */
	public Class<ILlmFeature> getFeatureType()
	{
		return ILlmFeature.class;
	}
	
	/**
	 *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
	 *  @param self	The component to which the feature belongs.
	 *  @return	The feature instance.
	 */
	public ILlmFeature createFeatureInstance()
	{
		return new LlmFeature();
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