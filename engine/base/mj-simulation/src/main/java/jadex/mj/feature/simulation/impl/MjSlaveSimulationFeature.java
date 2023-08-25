package jadex.mj.feature.simulation.impl;

import jadex.future.IFuture;
import jadex.mj.feature.execution.impl.MjExecutionFeature;

public class MjSlaveSimulationFeature extends MjExecutionFeature
{
	protected static MjMasterSimulationFeature	master	= new MjMasterSimulationFeature();
	
	@Override
	public long getTime()
	{
		return master.getTime();
	}
	
	@Override
	public void scheduleStep(Runnable r)
	{
		master.scheduleStep(this, r);
	}
	
	@Override
	public IFuture<Void> waitForDelay(long millis)
	{
		return master.waitForDelay(millis);
	}
}
