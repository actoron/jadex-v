package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
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
		BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<Void>	ret	= new Future<>();
			IComponentHandle	agent	= LambdaAgent.create(comp ->
			{
				comp.getComponentHandle().scheduleStep(() -> ret.setResult(null));
				new Future<Void>().get();
			});
			ret.get();
			agent.terminate().get();
		});
	}

	public static void	main(String[] args)
	{
		for(;;) 
		{
			Future<Void>	ret	= new Future<>();
			IComponentHandle	agent	= LambdaAgent.create(comp ->
			{
				comp.getComponentHandle().scheduleStep(() -> ret.setResult(null));
				new Future<Void>().get();
			});
			ret.get();
			agent.terminate().get();
		}
	}
}
