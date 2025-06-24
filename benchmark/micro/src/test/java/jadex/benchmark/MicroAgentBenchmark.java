package jadex.benchmark;


import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.injection.annotation.OnStart;

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
			IComponentHandle	agent	= IComponentManager.get().create(new Object()
			{
				@OnStart
				public void	start()
				{
					ret.setResult(null);
				}
			}).get();
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
			IComponentHandle	agent	= IComponentManager.get().create(new Object()
			{
				@OnStart
				public void	start()
				{
					ret.setResult(null);
				}
			}).get();
			ret.get();
			return () -> agent.terminate().get();
		});
	}

	public static void	main(String[] args)
	{
		for(;;)
		{
			Future<Void>	ret	= new Future<>();
			IComponentHandle	agent	= IComponentManager.get().create(new Object()
			{
				@OnStart
				public void	start()
				{
					ret.setResult(null);
				}
			}).get();
			ret.get();
			agent.terminate().get();
		}
	}
}
