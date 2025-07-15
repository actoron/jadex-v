package jadex.messaging.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.ComponentManager;
import jadex.messaging.IMessageFeature;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.impl.security.SecurityFeature;

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

	@Override
	public IMessageFeature createFeatureInstance(Component self)
	{
		// Ensure availability of security&IPC
		SecurityFeature sec = (SecurityFeature) ComponentManager.get().getFeature(ISecurityFeature.class);
		sec.loadLocalGroup();
		return new MessageFeature(self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType()
	{
		return Component.class;
	}
	
	@Override
	public boolean isLazyFeature()
	{
		return true;
	}
}
