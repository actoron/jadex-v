package jadex.execution.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadex.common.TimeoutException;
import jadex.core.impl.Component;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.simulation.ISimulationFeature;
import jadex.simulation.impl.MasterSimulationFeature;
import jadex.simulation.impl.SlaveSimulationFeature;

public class ParallelSimulationTest extends AbstractExecutionFeatureTest
{
	// hack for eclipse
	@BeforeEach
	public void	setup()
	{
		MasterSimulationFeature.master	= null;
		SlaveSimulationFeature.parallel	= true;
	}
	
	@Test
	public void	testStopWhenIdle()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(null));
		ISimulationFeature	sim	= ((ISimulationFeature)comp.getFeature(IExecutionFeature.class));
		sim.stop().get(TIMEOUT);
		assertThrows(IllegalStateException.class, () -> sim.stop().get(TIMEOUT));
	}
	
	@Test
	public void	testStopWhenExecuting()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(null));
		ISimulationFeature	sim	= ((ISimulationFeature)comp.getFeature(IExecutionFeature.class));
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
		stop.get(TIMEOUT);
	}

	@Test
	public void	testInverseOrder()
	{
//		System.out.println("testInverseOrder");
		Component	comp	= Component.createComponent(Component.class, () -> new Component(null));
		ISimulationFeature	sim	= ((ISimulationFeature)comp.getFeature(IExecutionFeature.class));
		sim.stop().get(TIMEOUT);
		List<String>	results	= new ArrayList<>();
		sim.waitForDelay(2000).then((v) -> results.add("A"));
		sim.waitForDelay(1000).then((v) -> results.add("B"));
		sim.start();
		sim.waitForDelay(4000).get(TIMEOUT);
		assertEquals(Arrays.asList("B", "A"), results);
	}

	@Test
	public void	testStart()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(null));
		ISimulationFeature	sim	= ((ISimulationFeature)comp.getFeature(IExecutionFeature.class));
		assertThrows(IllegalStateException.class, () -> sim.start());
		sim.stop().get(TIMEOUT);
		List<String>	results	= new ArrayList<>();
		sim.waitForDelay(100).then((v) -> results.add("A"));
		sim.waitForDelay(200).then((v) -> sim.stop().get());
		sim.waitForDelay(300).then((v) -> results.add("B"));
		sim.start();
		assertThrows(TimeoutException.class, () -> sim.waitForDelay(300).get(500));
		assertEquals(Arrays.asList("A"), results);
		sim.start();
		sim.waitForDelay(200).get(TIMEOUT);
		assertEquals(Arrays.asList("A", "B"), results);
	}

	@Test
	public void	testMultipleComponents()
	{
		String[]	input	= new String[]{"A", "B", "C", "D", "E", "F"};
		ISimulationFeature[]	sim	= new ISimulationFeature[input.length];
		StringBuffer	output	= new StringBuffer();
		
		for(int i=0; i<input.length; i++)
		{
			int num	= i;
			Component	comp	= Component.createComponent(Component.class, () -> new Component(null));
			sim[i]	= ((ISimulationFeature)comp.getFeature(IExecutionFeature.class));
			if(i==0)
			{
				sim[i].stop().get(TIMEOUT);
			}
			
			sim[i].scheduleStep(() -> 
			{
				IExecutionFeature.get().waitForDelay(num).get();
				
				for(int j=0; j<=num; j++)
				{
					sim[j].scheduleStep(() -> output.append(input[num]));
				}
			});
		}
		
		sim[0].start();
		sim[0].waitForDelay(1000).get(TIMEOUT);
		assertEquals("ABBCCCDDDDEEEEEFFFFFF", output.toString());
	}
}
