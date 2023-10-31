package jadex.mj.feature.execution.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadex.core.impl.Component;
import jadex.feature.execution.IExecutionFeature;
import jadex.mj.feature.simulation.ISimulationFeature;
import jadex.mj.feature.simulation.impl.SlaveSimulationFeature;

public class SequentialSimulationTest extends ParallelSimulationTest
{
	// hack for eclipse
	@BeforeEach
	public void	setup()
	{
		super.setup();
		SlaveSimulationFeature.parallel	= false;
	}
	
	@Test
	public void	testReproducibility()
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
				sim[i].stop().get(1000);
			}
			
			sim[i].scheduleStep(() -> 
			{
				IExecutionFeature.get().waitForDelay(num).get();
				
				for(int j=0; j<=num; j++)
				{
					int dum	= j;
					sim[j].scheduleStep(() -> output.append(input[dum]));
				}
			});
		}
		
		sim[0].start();
		sim[0].waitForDelay(1000).get(1000);
		assertEquals("AABABCABCDABCDEABCDEF", output.toString());
	}
}
