package jadex.messaging.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.messaging.IMessageFeature;

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
		return new MessageFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType() 
	{
		return Component.class;
	}
}
