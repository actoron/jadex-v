package jadex.injection.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.injection.IInjectionFeature;

/**
 *  Injection feature provider.
 */
public class InjectionFeatureProvider extends ComponentFeatureProvider<IInjectionFeature>
{
	@Override
	public Class<IInjectionFeature> getFeatureType()
	{
		return IInjectionFeature.class;
	}

	@Override
	public IInjectionFeature createFeatureInstance(Component self)
	{
		return new InjectionFeature(self);
	}
}
