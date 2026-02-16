package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentManager;

/**
 *  Benchmark simple lambda agent.
 */
public class LambdaAgentBenchmark
{
	@Test
	void benchmarkTime()
	{
		// Run inside component for better comparison to previous version without result scheduling
		// -> otherwise would schedule on global runner and then wake up to main thread
		// -> now it only wakes up caller component.
		IComponentManager.get().run((Runnable) () ->
			BenchmarkHelper.benchmarkTime(() -> 
			{
				IComponentManager.get().run(comp -> comp.getId()).get();
			})
		).get();
	}

	public static void main(String[] args)
	{
		for(;;)
		{
			IComponentManager.get().run(comp -> comp.getId()).get();
		}
	}
}
