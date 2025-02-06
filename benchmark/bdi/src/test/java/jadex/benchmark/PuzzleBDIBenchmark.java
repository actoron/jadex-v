package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bdi.puzzle.BenchmarkAgent;
import jadex.bdi.runtime.IBDIAgent;
import jadex.common.SUtil;
import jadex.core.IComponentHandle;

/**
 *  Benchmark the puzzle (Sokrates) example.
 */
public class PuzzleBDIBenchmark
{
	@Test
	public void	benchmarkTime()
	{
		double	pct	= BenchmarkHelper.benchmarkTime(() ->
		{
			BenchmarkAgent	agent	= new BenchmarkAgent(false);
			IComponentHandle	exta	= IBDIAgent.create(agent);
			exta.waitForTermination().get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}

	public static void main(String[] args)
	{
		for(int i=0; i<Runtime.getRuntime().availableProcessors()*2; i++)
		{
			SUtil.getExecutor().execute(() ->
			{
				for(;;)
				{
					BenchmarkAgent	agent	= new BenchmarkAgent(false);
					IComponentHandle	exta	= IBDIAgent.create(agent);
					exta.waitForTermination().get();
				}
			});
		}
	}
}
