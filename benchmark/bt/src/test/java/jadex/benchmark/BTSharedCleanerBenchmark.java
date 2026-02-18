package jadex.benchmark;

import java.lang.System.Logger.Level;

import org.junit.jupiter.api.Test;

import jadex.bt.cleanerworld.environment.CleanerworldEnvironment;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.environment.Environment;
import jadex.future.Future;
import jadex.logger.ILoggingFeature;

/**
 *  Benchmark creation and killing of bt agents with shared tree.
 */
public class BTSharedCleanerBenchmark
{
	static
	{
		IComponentManager.get().getFeature(ILoggingFeature.class).setSystemLoggingLevel(Level.ERROR);
		IComponentManager.get().getFeature(ILoggingFeature.class).setAppLoggingLevel(Level.WARNING);
	}
	
	@Test
	void benchmarkTime()
	{
		int fps = 0; // steps / frames per second: 0 -> disable steps
		IComponentHandle	env = IComponentManager.get().create(new CleanerworldEnvironment(fps)).get();
		env.getPojoHandle(CleanerworldEnvironment.class).createWorld().get();
		String envid = Environment.add(env.getPojoHandle(CleanerworldEnvironment.class));
		
		BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new BTSharedCleanerBenchmarkAgent(ret, envid)).get();
			ret.get();
			agent.terminate().get();
		});
		
		env.terminate().get();
	}

	@Test
	void benchmarkMemory()
	{
		int fps = 0; // steps / frames per second: 0 -> disable steps
		IComponentHandle	env = IComponentManager.get().create(new CleanerworldEnvironment(fps)).get();
		env.getPojoHandle(CleanerworldEnvironment.class).createWorld().get();
		String envid = Environment.add(env.getPojoHandle(CleanerworldEnvironment.class));
		
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new BTSharedCleanerBenchmarkAgent(ret, envid)).get();
			ret.get();
			return () -> agent.terminate().get();
		});
		
		env.terminate().get();
	}
	
	public static void main(String[] args)
	{
		new BTSharedCleanerBenchmark().benchmarkMemory();
	}
}
