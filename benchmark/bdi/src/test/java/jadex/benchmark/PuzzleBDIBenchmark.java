package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.bdi.puzzle.BenchmarkAgent;
import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IExternalAccess;

/**
 *  Benchmark the puzzle (Sokrates) example.
 */
public class PuzzleBDIBenchmark
{
	@Test
	public void	benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() ->
		{
			BenchmarkAgent	agent	= new BenchmarkAgent(false);
			IExternalAccess	exta	= IBDIAgent.create(agent);
			exta.waitForTermination().get();
		});
	}
}
