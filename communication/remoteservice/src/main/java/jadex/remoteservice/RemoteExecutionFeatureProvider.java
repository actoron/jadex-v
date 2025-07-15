package jadex.remoteservice;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.remoteservice.impl.RemoteExecutionFeature;

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

	/**
	 *  Returns the type of feature provided.
	 *  @return Feature type.
	 */
	@Override
	public Class<IRemoteExecutionFeature> getFeatureType()
	{
		return IRemoteExecutionFeature.class;
	}

	/**
	 *  Unchecked. Creates an instance of the remote execution feature.
	 *  @param self The component to which the feature belongs.
	 *  @return Feature instance.
	 */
	@Override
	public IRemoteExecutionFeature createFeatureInstance(Component self)
	{
		return new RemoteExecutionFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return Component.class;
	}
}
