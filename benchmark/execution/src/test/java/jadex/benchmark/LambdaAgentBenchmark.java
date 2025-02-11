package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
		double pct	= BenchmarkHelper.benchmarkTime(() -> 
		{
			IComponentManager.get().run(comp ->{return comp.getId();}).get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}
}
