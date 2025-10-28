package jadex.bdi.impl;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.future.IFuture;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.InjectionFeatureProvider;

/**
 *  Handle BDI agent creation etc.
 */
public class BDIInjectionFeatureProvider extends ComponentFeatureProvider<IInjectionFeature> implements IComponentLifecycleManager
{
	@Override
	public IInjectionFeature createFeatureInstance(Component self)
	{
		return new BDIInjectionFeature((BDIAgent)self);
	}
	
	@Override
	public Class<IInjectionFeature> getFeatureType()
	{
		return IInjectionFeature.class;
	}
	
	@Override
	public boolean replacesFeatureProvider(ComponentFeatureProvider<?> provider)
	{
		return provider instanceof InjectionFeatureProvider;
	}
	
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return BDIAgent.class;
	}

	@Override
	public int isCreator(Class<?> pojoclazz)
	{
		return -1;
	}

	@Override
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return null;
	}
}
