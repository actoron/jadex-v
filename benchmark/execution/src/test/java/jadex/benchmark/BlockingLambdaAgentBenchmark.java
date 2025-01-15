package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.core.IExternalAccess;
import jadex.execution.LambdaAgent;
import jadex.future.Future;

/**
 *  Benchmark an agent that blocks a thread.
 */
public class BlockingLambdaAgentBenchmark
{
	@Test
	void	benchmarkTime()
	{
		double pct	= BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<Void>	ret	= new Future<>();
			IExternalAccess	agent	= LambdaAgent.create(comp ->
			{
				comp.getExternalAccess().scheduleStep(() -> ret.setResult(null));
				new Future<Void>().get();
			});
			ret.get();
			agent.terminate().get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}
}
