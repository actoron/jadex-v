package jadex.mj.feature.simulation.impl;

import jadex.core.impl.FeatureProvider;
import jadex.feature.execution.IExecutionFeature;
import jadex.feature.execution.impl.ExecutionFeature;
import jadex.feature.execution.impl.ExecutionFeatureProvider;

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
