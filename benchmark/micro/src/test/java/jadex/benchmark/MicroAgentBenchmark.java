package jadex.benchmark;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
import jadex.future.Future;
import jadex.micro.MicroAgent;
import jadex.model.annotation.OnStart;

/**
 *  Benchmark creation and killing of micro agents.
 */
public class MicroAgentBenchmark 
{
	@Test
	void	benchmarkTime()
	{
		double pct	= BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<Void>	ret	= new Future<>();
			IComponentHandle	agent	= MicroAgent.create(new Object()
			{
				@OnStart
				public void	start()
				{
					ret.setResult(null);
				}
			});
			ret.get();
			agent.terminate().get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}

	@Test
	void	benchmarkMemory()
	{
		double pct	= BenchmarkHelper.benchmarkMemory(() -> 
		{
			Future<Void>	ret	= new Future<>();
			IComponentHandle	agent	= MicroAgent.create(new Object()
			{
				@OnStart
				public void	start()
				{
					ret.setResult(null);
				}
			});
			ret.get();
			return () -> agent.terminate().get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}

}
