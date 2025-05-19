package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentManager;

/**
 *  Benchmark simple lambda agent.
 */
public class LambdaAgentBenchmark
{
	@Test
	void benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			IComponentManager.get().run(comp ->{return comp.getId();}).get();
		});
	}

	public static void main(String[] args)
	{
		for(;;)
		{
			IComponentManager.get().run(comp ->{return comp.getId();}).get();
		}
	}
}
