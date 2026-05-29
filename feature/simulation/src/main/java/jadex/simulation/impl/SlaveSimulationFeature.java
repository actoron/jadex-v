package jadex.simulation.impl;

import jadex.core.impl.Component;
import jadex.execution.impl.ExecutionFeature;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.simulation.ISimulationFeature;

public class SlaveSimulationFeature extends ExecutionFeature	implements ISimulationFeature
{
	protected MasterSimulationFeature	master;
	
	// Hack!!! public to allow reset for testing in eclipse
	public static boolean	parallel	= true;
	
	/**
	 *  Create a slave simulation feature.
	 */
	public SlaveSimulationFeature(Component component)
	{
		super(component);
	}
	
	/**
	 *  Get the appropriate master for this component.
	 */
	protected MasterSimulationFeature	getMaster()
	{
		if(master==null)
		{
			master	= MasterSimulationFeature.getMaster();
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
	public ITerminableFuture<Void> waitForDelay(long millis)
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
	
//	volatile boolean	busy	= false;
	
	@Override
	protected void busy()
	{
		assert parallel;
//		if(busy)
//			throw new IllegalStateException("Already busy!");
//		busy	= true;
		getMaster().componentBusy();
	}
	
	@Override
	protected void idle()
	{
		assert parallel;
//		if(!busy)
//			throw new IllegalStateException("Not busy!");
//		busy	= false;
		getMaster().componentIdle();
	}
}
