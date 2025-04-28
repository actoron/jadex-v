package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentManager;

/**
 *  Benchmark plain MjComponent with included execution feature.
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
}
