package jadex.benchmark;

import java.lang.System.Logger.Level;

import org.junit.jupiter.api.Test;

import jadex.bdi.cleanerworld.environment.CleanerworldEnvironment;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.environment.Environment;
import jadex.future.Future;
import jadex.logger.ILoggingFeature;

public class CleanerBDIBenchmark
{
	static String envid;
	static
	{
		IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultSystemLoggingLevel(Level.ERROR);
		IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultAppLoggingLevel(Level.WARNING);

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
			IComponentHandle agent = IComponentManager.get().create(new BenchmarkCleanerBDIAgent(ret, envid)).get();
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
			IComponentHandle agent = IComponentManager.get().create(new BenchmarkCleanerBDIAgent(ret, envid)).get();
			ret.get();
			return () -> agent.terminate().get();
		});
	}
}
