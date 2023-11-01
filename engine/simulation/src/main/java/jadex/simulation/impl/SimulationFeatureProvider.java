package jadex.simulation.impl;

import jadex.core.impl.FeatureProvider;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ExecutionFeature;
import jadex.execution.impl.ExecutionFeatureProvider;

public class SimulationFeatureProvider extends ExecutionFeatureProvider
{
	@Override
	public boolean replacesFeatureProvider(FeatureProvider<IExecutionFeature> provider)
	{
		return provider instanceof ExecutionFeatureProvider;
	}
	
	@Override
	protected ExecutionFeature doCreateFeatureInstance()
	{
		return new SlaveSimulationFeature();
	}
}
