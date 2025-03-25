package jadex.featuretest.impl;

import java.util.Map;
import java.util.function.Supplier;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IBootstrapping;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.featuretest.BootstrappingTest;
import jadex.featuretest.ITestFeature2;

public class TestFeature2NewProvider extends ComponentFeatureProvider<ITestFeature2> implements ITestFeature2, IBootstrapping, IComponentLifecycleManager
{
	public static class SubComponent	extends Component
	{
		public SubComponent(Object pojo)
		{
			super(pojo);
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
	public boolean replacesFeatureProvider(ComponentFeatureProvider<ITestFeature2> provider)
	{
		return provider.getClass().equals(TestFeature2Provider.class);
	}

	@Override
	public <T extends Component> T bootstrap(Class<T> type, Supplier<T> creator)
	{
		BootstrappingTest.bootstraps.add(getClass().getSimpleName()+"_beforeCreate");
		T	ret	= creator.get();
		BootstrappingTest.bootstraps.add(getClass().getSimpleName()+"_afterCreate");
		return ret;

	}


	@Override
	public int isCreator(Class<?> pojoclazz)
	{
		return Double.class.equals(pojoclazz) ? 2: -1;
	}

	@Override
	public IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return 	Component.createComponent(SubComponent.class, () -> new SubComponent(pojo)).getComponentHandle();
	}

	@Override
	public void terminate(IComponent component)
	{
		((IInternalExecutionFeature)component.getFeature(IExecutionFeature.class)).terminate();
	}

	@Override
	public Map<String, Object> getResults(Object pojo)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
