package jadex.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadex.common.TimeoutException;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
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
	
	// hack for eclipse
	@AfterAll
	public static void	teardown()
	{
		MasterSimulationFeature.master	= null;
		SlaveSimulationFeature.parallel	= true;
	}
	
	@Test
	public void	testStopWhenIdle()
	{
		IComponentHandle	comp	= IComponentManager.get().create(null).get(TIMEOUT);
		ISimulationFeature	sim	= (ISimulationFeature)comp.scheduleStep(c->{return c.getFeature(IExecutionFeature.class);}).get(TIMEOUT);
		sim.stop().get(TIMEOUT);
		assertThrows(IllegalStateException.class, () -> sim.stop().get(TIMEOUT));
	}
	
	@Test
	public void	testStopWhenExecuting()
	{
		IComponentHandle	comp	= IComponentManager.get().create(null).get(TIMEOUT);
		ISimulationFeature	sim	= (ISimulationFeature)comp.scheduleStep(c->{return c.getFeature(IExecutionFeature.class);}).get(TIMEOUT);
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
		IComponentHandle	comp	= IComponentManager.get().create(null).get(TIMEOUT);
		ISimulationFeature	sim	= (ISimulationFeature)comp.scheduleStep(c->{return c.getFeature(IExecutionFeature.class);}).get(TIMEOUT);
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
		IComponentHandle	comp	= IComponentManager.get().create(null).get(TIMEOUT);
		ISimulationFeature	sim	= (ISimulationFeature)comp.scheduleStep(c->{return c.getFeature(IExecutionFeature.class);}).get(TIMEOUT);
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
			IComponentHandle	comp	= IComponentManager.get().create(null).get(TIMEOUT);
			sim[i]	= (ISimulationFeature)comp.scheduleStep(c->{return c.getFeature(IExecutionFeature.class);}).get(TIMEOUT);
			if(i==0)
			{
				sim[i].stop().get(TIMEOUT);
			}
			
			sim[i].scheduleStep(() -> 
			{
				IExecutionFeature.get().waitForDelay(num).get();
				
				for(int j=0; j<=num; j++)
				{
					sim[j].scheduleStep((Runnable)() -> output.append(input[num]));
				}
			});
		}
		
		sim[0].start();
		sim[0].waitForDelay(1000).get(TIMEOUT);
		assertEquals("ABBCCCDDDDEEEEEFFFFFF", output.toString());
	}
}
