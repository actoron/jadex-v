package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
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
			Future<IComponentHandle>	ret	= new Future<>();
			IComponentManager.get().run(comp ->
			{
				comp.getFeature(IExecutionFeature.class).scheduleStep(
					() -> ret.setResult(comp.getComponentHandle()));
				return new Future<Void>().get();
			});
			ret.get().terminate().get();
		});
	}

	public static void	main(String[] args)
	{
		for(;;) 
		{
			Future<IComponentHandle>	ret	= new Future<>();
			IComponentManager.get().run(comp ->
			{
				comp.getFeature(IExecutionFeature.class).scheduleStep(
					() -> ret.setResult(comp.getComponentHandle()));
				return new Future<Void>().get();
			});
			ret.get().terminate().get();
		}
	}
}
