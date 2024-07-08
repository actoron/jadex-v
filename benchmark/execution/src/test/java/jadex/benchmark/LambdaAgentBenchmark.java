package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.core.ComponentIdentifier;
import jadex.execution.LambdaAgent;
import jadex.execution.LambdaAgent.Result;

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
			Result<ComponentIdentifier>	result	= LambdaAgent.create(comp ->{return comp.getId();});
			result.result().get();
			result.component().terminate().get();
		});
		assertTrue(pct<20);	// Fail when more than 20% worse
	}
}
