package jadex.mj.feature.execution.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.simulation.IMjSimulationFeature;
import jadex.mj.feature.simulation.impl.MjSlaveSimulationFeature;

public class SequentialSimulationTest extends ParallelSimulationTest
{
	// hack for eclipse
	@BeforeEach
	public void	setup()
	{
		super.setup();
		MjSlaveSimulationFeature.parallel	= false;
	}
	
	@Test
	public void	testReproducibility()
	{
		String[]	input	= new String[]{"A", "B", "C", "D", "E", "F"};
		IMjSimulationFeature[]	sim	= new IMjSimulationFeature[input.length];
		StringBuffer	output	= new StringBuffer();
		
		for(int i=0; i<input.length; i++)
		{
			int num	= i;
			MjComponent	comp	= MjComponent.createComponent(MjComponent.class, () -> new MjComponent(null));
			sim[i]	= ((IMjSimulationFeature)IMjExecutionFeature.getExternal(comp));
			if(i==0)
			{
				sim[i].stop().get(1000);
			}
			
			sim[i].scheduleStep(() -> 
			{
				IMjExecutionFeature.get().waitForDelay(num).get();
				
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
