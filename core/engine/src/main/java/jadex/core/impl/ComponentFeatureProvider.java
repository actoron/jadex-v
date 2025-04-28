package jadex.core.impl;

import java.util.Collections;
import java.util.Set;

import jadex.core.IComponentFeature;

/**
 *  A feature provider is loaded from the classpath and adds a specific feature to components.
 */
public abstract class ComponentFeatureProvider<T extends IComponentFeature> extends FeatureProvider<T>
{
	/**
	 *  If the feature does not apply to all kinds of components,
	 *  you can provide a specific subtype and then this feature
	 *  is only added to components of this type (e.g. BDI features
	 *  only for BDI agents).
	 *  @return	A subtype of {@link Component}.
	 */
	public Class<? extends Component> getRequiredComponentType()
	{
		return Component.class;
	}
	
	/**
	 *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
	 *  @param self	The component to which the feature belongs.
	 *  @return	The feature instance.
	 */
	public abstract T createFeatureInstance(Component self);
	
	/**
	 *  Allow a feature implementation to replace another implementation.
	 *  This method is called for any feature provider, where two or more different
	 *  providers are in the classpath, which apply to the same component type {@link #getRequiredComponentType()}
	 *  and have the same {@link #getFeatureType()}.
	 *  @return	True, if this implementation should be used and the other one ignored. 
	 */
	public boolean replacesFeatureProvider(ComponentFeatureProvider<T> provider)
	{
		return false;
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
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		return Collections.emptySet();
	}
}
