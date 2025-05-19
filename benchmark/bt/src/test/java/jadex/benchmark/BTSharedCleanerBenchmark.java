package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.bt.cleanerworld.environment.CleanerworldEnvironment;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.environment.Environment;
import jadex.future.Future;

/**
 *  Benchmark creation and killing of bt agents with shared tree.
 */
public class BTSharedCleanerBenchmark	extends BaseTest
{
	static String envid;
	static
	{
		int fps = 5; // steps / frames per second
		CleanerworldEnvironment env = IComponentManager.get().create(new CleanerworldEnvironment(fps)).get().getPojoHandle(CleanerworldEnvironment.class);
		env.createWorld().get();
		envid = Environment.add(env);
	}
	
	@Test
	void benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new BTSharedCleanerBenchmarkAgent(ret, envid)).get();
			ret.get();
			agent.terminate().get();
		}, 50);
	}

	@Test
	void benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new BTSharedCleanerBenchmarkAgent(ret, envid)).get();
			ret.get();
			return () -> agent.terminate().get();
		}, 50);
	}
}
