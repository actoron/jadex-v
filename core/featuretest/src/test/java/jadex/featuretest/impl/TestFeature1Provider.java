package jadex.featuretest.impl;

import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.featuretest.ITestFeature1;
import jadex.future.IFuture;

public class TestFeature1Provider extends ComponentFeatureProvider<ITestFeature1> implements ITestFeature1, IComponentLifecycleManager
{
	@Override
	public Class<ITestFeature1> getFeatureType()
	{
		return ITestFeature1.class;
	}

	@Override
	public ITestFeature1 createFeatureInstance(Component self)
	{
		return this;
	}

	@Override
	public int isCreator(Class<?> pojoclazz)
	{
		return SReflect.isSupertype(Number.class, pojoclazz) ? 1: -1;
	}

	@Override
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(Component.class, () -> new Component(pojo, cid, app));
	}

	@Override
	public void terminate(IComponent component)
	{
		((IInternalExecutionFeature)component.getFeature(IExecutionFeature.class)).terminate();
	}
}
