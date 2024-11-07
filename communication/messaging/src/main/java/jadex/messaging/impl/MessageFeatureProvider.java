package jadex.messaging.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.ComponentManager;
import jadex.messaging.IMessageFeature;
import jadex.messaging.ISecurityFeature;

/**
 *  Provider class for the message component feature.
 */
public class MessageFeatureProvider extends ComponentFeatureProvider<IMessageFeature> 
{
	/**
	 *  Create MessageFeatureProvider.
	 */
	public MessageFeatureProvider()
	{
	}
	
	@Override
	public Class<IMessageFeature> getFeatureType()
	{
		return IMessageFeature.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public IMessageFeature createFeatureInstance(Component self)
	{
		// Ensure availability of security&IPC
		ComponentManager.get().getFeature(ISecurityFeature.class);
		return new MessageFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return Component.class;
	}
}
