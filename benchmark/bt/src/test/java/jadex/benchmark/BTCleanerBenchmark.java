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
 *  Benchmark creation and killing of bt agents.
 */
public class BTCleanerBenchmark
{
	static String envid;
	static
	{
		IComponentManager.get().getFeature(ILoggingFeature.class).setSystemLoggingLevel(Level.ERROR);
		IComponentManager.get().getFeature(ILoggingFeature.class).setAppLoggingLevel(Level.WARNING);

		int fps = 0; // steps / frames per second: 0 -> disable steps
		CleanerworldEnvironment env = IComponentManager.get().create(new CleanerworldEnvironment(fps)).get().getPojoHandle(CleanerworldEnvironment.class);
		env.createWorld().get();
		envid = Environment.add(env);
	}
	
//	@Test
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

//	@Test
	void benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new BTCleanerBenchmarkAgent(ret, envid)).get();
			ret.get();
			return () -> agent.terminate().get();
		});
	}
	
	public static void	main(String[] args)
	{
		for(;;) 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new BTCleanerBenchmarkAgent(ret, envid)).get();
			ret.get();
			agent.terminate().get();
		}
	}
}
