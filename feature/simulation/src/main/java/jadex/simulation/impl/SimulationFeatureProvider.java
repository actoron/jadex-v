package jadex.simulation.impl;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.execution.impl.ExecutionFeature;
import jadex.execution.impl.ExecutionFeatureProvider;

public class SimulationFeatureProvider extends ExecutionFeatureProvider
{
	@Override
	public boolean replacesFeatureProvider(ComponentFeatureProvider<?> provider)
	{
		return provider instanceof ExecutionFeatureProvider;
	}
	
	@Override
	protected ExecutionFeature doCreateFeatureInstance(Component component)
	{
		return new SlaveSimulationFeature(component);
	}
}
