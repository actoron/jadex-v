package jadex.mj.core.impl;

import jadex.mj.core.MjComponent;

/**
 *  A feature providers is loaded from the classpath and adds a specific feature to components.
 */
public abstract class MjFeatureProvider<T>
{
	/**
	 *  Get the type of the feature used for accessing.
	 */
	public abstract Class<T>	getFeatureType();
	
	/**
	 *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
	 *  @param self	The component to which the feature belongs.
	 *  @return	The feature instance.
	 */
	public abstract T	createFeatureInstance(MjComponent self);
	
	/**
	 *  If the feature does not apply to all kinds of components,
	 *  you can provide a specific subtype and then this feature
	 *  is only added to components of this type (e.g. BDI features
	 *  only for BDI agents).
	 *  @return	A subtype of {@link MjComponent}.
	 */
	public Class<? extends MjComponent>	getRequiredComponentType()
	{
		return MjComponent.class;
	}
	
	/**
	 *  Determines if the feature is created immediately
	 *  on component startup (false) or later on first access (true).
	 *  @return	Defaults to false.
	 */
	public boolean	isLazyFeature()
	{
		return false;
	}
}
