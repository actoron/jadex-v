package jadex.featuretest.impl;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.featuretest.ITestFeature2;
import jadex.future.IFuture;

public class TestFeature2NewProvider extends ComponentFeatureProvider<ITestFeature2> implements ITestFeature2, IComponentLifecycleManager
{
	public static class SubComponent	extends Component
	{
		public SubComponent(Object pojo, ComponentIdentifier cid, Application app)
		{
			super(pojo, cid, app);
		}
	}
	
	@Override
	public Class<ITestFeature2> getFeatureType()
	{
		return ITestFeature2.class;
	}

	@Override
	public ITestFeature2 createFeatureInstance(Component self)
	{
		return this;
	}
	
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return SubComponent.class;
	}
	
	@Override
	public boolean replacesFeatureProvider(ComponentFeatureProvider<?> provider)
	{
		return provider.getClass().equals(TestFeature2Provider.class);
	}

	@Override
	public int isCreator(Class<?> pojoclazz)
	{
		return Double.class.equals(pojoclazz) ? 2: -1;
	}

	@Override
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return 	Component.createComponent(new SubComponent(pojo, cid, app));
	}
}
