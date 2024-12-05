package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.bdi.puzzle.BenchmarkAgent;
import jadex.bdi.runtime.IBDIAgent;
import jadex.common.SUtil;
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

	public static void main(String[] args)
	{
		for(int i=0; i<Runtime.getRuntime().availableProcessors()*2; i++)
		{
			SUtil.getExecutor().execute(() ->
			{
				for(;;)
				{
					BenchmarkAgent	agent	= new BenchmarkAgent(false);
					IExternalAccess	exta	= IBDIAgent.create(agent);
					exta.waitForTermination().get();
				}
			});
		}
	}
}
