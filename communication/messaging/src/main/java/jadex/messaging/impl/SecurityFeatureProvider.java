package jadex.messaging.impl;

import java.util.HashSet;
import java.util.Set;

import jadex.core.IComponentManager;
import jadex.core.IRuntimeFeature;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.core.impl.RuntimeFeatureProvider;
import jadex.messaging.IIpcFeature;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.impl.security.SecurityFeature;

public class SecurityFeatureProvider extends RuntimeFeatureProvider<ISecurityFeature>
{
	/**
	 *  Get the type of the feature used for accessing.
	 */
	public Class<ISecurityFeature> getFeatureType()
	{
		return ISecurityFeature.class;
	}
	
	/**
	 *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
	 *  @param self	The component to which the feature belongs.
	 *  @return	The feature instance.
	 */
	public ISecurityFeature createFeatureInstance()
	{
		IIpcFeature ipc = IComponentManager.get().getFeature(IIpcFeature.class);
		ISecurityFeature ret = new SecurityFeature(GlobalProcessIdentifier.getSelf(), ipc);
		return ret;
	}
	
	/**
	 *  Get the feature dependencies, i.e. features that are required to be available
	 *  before this one can be requested.
	 *  @return The dependencies.
	 */
	public Set<Class<? extends IRuntimeFeature>> getDependencies()
	{
		Set<Class<? extends IRuntimeFeature>> ret = new HashSet<>();
		ret.add(IIpcFeature.class);
		return ret;
	}
}
