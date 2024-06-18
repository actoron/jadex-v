package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.execution.LambdaAgent;

/**
 *  Benchmark plain MjComponent with included execution feature.
 */
public class LambdaAgentBenchmark
{
	@Test
	void	benchmarkTime()
	{
		double pct	= BenchmarkHelper.benchmarkTime(() -> 
		{
			LambdaAgent.create(comp -> {return comp.getId();}).result().get();
		});
		assertTrue(pct<20);	// Fail when more than 20% worse
	}
}
