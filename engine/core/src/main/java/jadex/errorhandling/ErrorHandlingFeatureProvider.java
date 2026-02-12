package jadex.errorhandling;

import jadex.core.impl.RuntimeFeatureProvider;

public class ErrorHandlingFeatureProvider extends RuntimeFeatureProvider<IErrorHandlingFeature>
{
	/**
	 *  Get the type of the feature used for accessing.
	 */
	public Class<IErrorHandlingFeature> getFeatureType()
	{
		return IErrorHandlingFeature.class;
	}
	
	/**
	 *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
	 *  @param self	The component to which the feature belongs.
	 *  @return	The feature instance.
	 */
	public IErrorHandlingFeature createFeatureInstance()
	{
		return new ErrorHandlingFeature();
	}
	
	/**
	 *  Get the feature dependencies, i.e. features that are required to be available
	 *  before this one can be requested.
	 *  @return The dependencies.
	 * /
	public Set<Class<? extends IRuntimeFeature>> getDependencies()
	{
		Set<Class<? extends IRuntimeFeature>> ret = new HashSet<>();
		ret.add(IIpcFeature.class);
		return ret;
	}*/
}
