package jadex.remoteservice.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.remoteservice.IRemoteExecutionFeature;

/**
 *  Provider class for the message component feature.
 */
public class RemoteExecutionFeatureProvider extends ComponentFeatureProvider<IRemoteExecutionFeature>
{
	/**
	 *  Create MessageFeatureProvider.
	 */
	public RemoteExecutionFeatureProvider()
	{
	}
	
	@Override
	public Class<IRemoteExecutionFeature> getFeatureType()
	{
		return IRemoteExecutionFeature.class;
	}

	@Override
	public IRemoteExecutionFeature createFeatureInstance(Component self)
	{
		return new jadex.remoteservice.impl.RemoteExecutionFeature(self);
	}
	
	@Override
	public boolean isLazyFeature()
	{
		return true; // Lazy feature, so that it is only created when needed.
	}
}
