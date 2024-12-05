package jadex.simulation.impl;

import jadex.core.impl.ComponentFeatureProvider;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ExecutionFeature;
import jadex.execution.impl.ExecutionFeatureProvider;

public class SimulationFeatureProvider extends ExecutionFeatureProvider
{
	@Override
	public boolean replacesFeatureProvider(ComponentFeatureProvider<IExecutionFeature> provider)
	{
		return provider instanceof ExecutionFeatureProvider;
	}
	
	@Override
	protected ExecutionFeature doCreateFeatureInstance()
	{
		return new SlaveSimulationFeature();
	}
}
