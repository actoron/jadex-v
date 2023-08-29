package jadex.mj.feature.execution.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadex.common.TimeoutException;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.SComponentFactory;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.simulation.IMjSimulationFeature;
import jadex.mj.feature.simulation.impl.MjSlaveSimulationFeature;

public class SimulationFeatureTest extends AbstractExecutionFeatureTest
{
	// hack for eclipse
	@BeforeEach
	public void	setup()
	{
		MjSlaveSimulationFeature.master	= null;
	}
	
	@Test
	public void	testStopWhenIdle()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		IMjSimulationFeature	sim	= ((IMjSimulationFeature)IMjExecutionFeature.getExternal(comp));
		sim.stop().get(1000);
		assertThrows(IllegalStateException.class, () -> sim.stop().get(1000));
	}
	
	@Test
	public void	testStopWhenExecuting()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		IMjSimulationFeature	sim	= ((IMjSimulationFeature)IMjExecutionFeature.getExternal(comp));
		boolean[]	run	= new boolean[]{true};
		sim.scheduleStep(() ->
		{
			while(run[0])
			{
				try{ Thread.sleep(10); }
				catch(InterruptedException e){}
			}
		});
		IFuture<Void>	stop	= sim.stop();
		run[0]	= false;
		stop.get(1000);
	}

	@Test
	public void	testInverseOrder()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		IMjSimulationFeature	sim	= ((IMjSimulationFeature)IMjExecutionFeature.getExternal(comp));
		sim.stop().get(1000);
		List<String>	results	= new ArrayList<>();
		sim.waitForDelay(2000).then((v) -> results.add("A"));
		sim.waitForDelay(1000).then((v) -> results.add("B"));
		sim.start();
		sim.waitForDelay(3000).get(1000);
		assertEquals(Arrays.asList("B", "A"), results);
	}

	@Test
	public void	testStart()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		IMjSimulationFeature	sim	= ((IMjSimulationFeature)IMjExecutionFeature.getExternal(comp));
		assertThrows(IllegalStateException.class, () -> sim.start());
		sim.stop().get(1000);
		List<String>	results	= new ArrayList<>();
		sim.waitForDelay(1000).then((v) -> results.add("A"));
		sim.waitForDelay(2000).then((v) -> sim.stop().get());
		sim.waitForDelay(3000).then((v) -> results.add("B"));
		sim.start();
		assertThrows(TimeoutException.class, () -> sim.waitForDelay(3000).get(10));
		assertEquals(Arrays.asList("A"), results);
		sim.start();
		sim.waitForDelay(2000).get(1000);
		assertEquals(Arrays.asList("A", "B"), results);
	}

//	@Test
//	public void	testMultipleComponents()
//	{
//		String[]	input	= new String[]{"A", "B", "C", "D", "E", "F"};
//		List<String>	output	= new ArrayList<>();
//		
//		for(int i=0; i<input.length; i++)
//		{
//			MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
//			IMjSimulationFeature	sim	= ((IMjSimulationFeature)IMjExecutionFeature.getExternal(comp));
//			sim.scheduleStep(() -> 
//			{
//				IMjExecutionFeature.get().waitForDelay(i);
//				output.add(input[i]);
//			});
//		}
//	}
}
