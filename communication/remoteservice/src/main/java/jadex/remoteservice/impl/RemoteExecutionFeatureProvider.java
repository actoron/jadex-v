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

	@SuppressWarnings("unchecked")
	@Override
	public IRemoteExecutionFeature createFeatureInstance(Component self)
	{
		return new jadex.remoteservice.impl.RemoteExecutionFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return Component.class;
	}
}
