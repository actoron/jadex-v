package jadex.mj.feature.simulation.impl;

import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.execution.impl.MjExecutionFeature;
import jadex.mj.feature.execution.impl.MjExecutionFeatureProvider;

public class MjSimulationFeatureProvider extends MjExecutionFeatureProvider
{
	@Override
	public boolean replacesFeatureProvider(MjFeatureProvider<IMjExecutionFeature> provider)
	{
		return provider instanceof MjExecutionFeatureProvider;
	}
	
	@Override
	protected MjExecutionFeature doCreateFeatureInstance()
	{
		return new MjSlaveSimulationFeature();
	}
}
