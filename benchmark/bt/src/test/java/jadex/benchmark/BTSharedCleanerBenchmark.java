package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;

/**
 *  Benchmark creation and killing of bt agents with shared tree.
 */
public class BTSharedCleanerBenchmark	extends BTAbstractCleanerBenchmark
{
	@Test
	void benchmarkTime()
	{
//		BenchmarkHelper.benchmarkTime(() -> 
//		{
//			Future<Void> ret = new Future<>();
//			IComponentHandle agent = IComponentManager.get().create(new BTSharedCleanerBenchmarkAgent(ret, envid)).get();
//			ret.get();
//			agent.terminate().get();
//		});
	}

	@Test
	void benchmarkMemory()
	{
//		BenchmarkHelper.benchmarkMemory(() -> 
//		{
//			Future<Void> ret = new Future<>();
//			IComponentHandle agent = IComponentManager.get().create(new BTSharedCleanerBenchmarkAgent(ret, envid)).get();
//			ret.get();
//			return () -> agent.terminate().get();
//		});
	}
}
