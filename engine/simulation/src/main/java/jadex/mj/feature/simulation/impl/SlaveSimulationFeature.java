package jadex.mj.feature.simulation.impl;

import jadex.future.IFuture;
import jadex.feature.execution.impl.ExecutionFeature;
import jadex.mj.feature.simulation.ISimulationFeature;

public class SlaveSimulationFeature extends ExecutionFeature	implements ISimulationFeature
{
	// Hack!!! public to allow reset for testing in eclipse
	public static volatile MasterSimulationFeature	master;
	
	// Hack!!! public to allow reset for testing in eclipse
	public static boolean	parallel	= true;
	
	/**
	 *  Get the appropriate master for this component.
	 */
	protected MasterSimulationFeature	getMaster()
	{
		if(master==null)
		{
			synchronized(this.getClass())
			{
				if(master==null)
				{
					master = new MasterSimulationFeature();
				}
			}
		}
		return master;
	}
	
	@Override
	public long getTime()
	{
		return getMaster().getTime();
	}
	
	@Override
	public void setTime(long millis)
	{
		getMaster().setTime(millis);
	}
	
	@Override
	public void scheduleStep(Runnable r)
	{
		if(parallel)
		{
			super.scheduleStep(r);
		}
		else
		{
			getMaster().scheduleStep(this, r);
		}
	}
	
	@Override
	public IFuture<Void> waitForDelay(long millis)
	{
		return getMaster().waitForDelay(millis);
	}
	
	@Override
	public void start()
	{
		getMaster().start();
	}
	
	@Override
	public IFuture<Void> stop()
	{
		return getMaster().stop();
	}
	
	@Override
	protected void busy()
	{
		assert parallel;
		getMaster().componentBusy();
	}
	
	@Override
	protected void idle()
	{
		assert parallel;
		getMaster().componentIdle();
	}
}
