package jadex.benchmark;


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
		BenchmarkHelper.benchmarkTime(() -> 
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
	}

	@Test
	void	benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() -> 
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
	}

}
