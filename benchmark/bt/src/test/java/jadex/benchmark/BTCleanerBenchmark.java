package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.bt.cleanerworld.environment.CleanerworldEnvironment;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.environment.Environment;
import jadex.future.Future;

/**
 *  Benchmark creation and killing of bt agents.
 */
public class BTCleanerBenchmark	extends BaseTest
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
			IComponentHandle agent = IComponentManager.get().create(new BTCleanerBenchmarkAgent(ret, envid)).get();
			ret.get();
			agent.terminate().get();
		});
	}

	@Test
	void benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new BTCleanerBenchmarkAgent(ret, envid)).get();
			ret.get();
			return () -> agent.terminate().get();
		}, 100);
	}
}
