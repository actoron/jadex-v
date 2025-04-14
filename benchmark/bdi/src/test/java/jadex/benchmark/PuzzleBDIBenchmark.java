package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.bdi.puzzle.BenchmarkAgent;
import jadex.common.SUtil;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;

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
			IComponentHandle	exta	= IComponentManager.get().create(agent).get();
			exta.waitForTermination().get();
		}, 50);	// Fail only when more than 50% worse as benchmark execution time varies a lot
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
					IComponentHandle	exta	= IComponentManager.get().create(agent).get();
					exta.waitForTermination().get();
				}
			});
		}
	}
}
