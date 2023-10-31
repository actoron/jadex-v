package jadex.mj.feature.simulation.impl;

import jadex.mj.core.impl.FeatureProvider;
import jadex.mj.feature.execution.IExecutionFeature;
import jadex.mj.feature.execution.impl.ExecutionFeature;
import jadex.mj.feature.execution.impl.ExecutionFeatureProvider;

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
