package jadex.benchmark;

import java.lang.System.Logger.Level;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import jadex.bt.cleanerworld.environment.CleanerworldEnvironment;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.environment.Environment;
import jadex.logger.ILoggingFeature;

/**
 *  Benchmark creation and killing of bt agents with shared tree.
 */
public class BTAbstractCleanerBenchmark
{
	IComponentHandle env;
	String envid;
	Level lsystem;
	Level lapp;

	@BeforeEach
	public void beforeEach()
	{
		int fps = 0; // steps / frames per second: 0 -> disable steps
		env = IComponentManager.get().create(new CleanerworldEnvironment(fps)).get();
//		env.getPojoHandle(CleanerworldEnvironment.class).createWorld().get();
//		envid = Environment.add(env.getPojoHandle(CleanerworldEnvironment.class));
		
//		lsystem = IComponentManager.get().getFeature(ILoggingFeature.class).getSystemLoggingLevel();
//		lapp = IComponentManager.get().getFeature(ILoggingFeature.class).getAppLogginglevel();
//		IComponentManager.get().getFeature(ILoggingFeature.class).setSystemLoggingLevel(Level.ERROR);
//		IComponentManager.get().getFeature(ILoggingFeature.class).setAppLoggingLevel(Level.WARNING);
	}
	
	@AfterEach
	public void afterEach()
	{
		env.terminate().get();
//		IComponentManager.get().getFeature(ILoggingFeature.class).setSystemLoggingLevel(lsystem);
//		IComponentManager.get().getFeature(ILoggingFeature.class).setAppLoggingLevel(lapp);
	}
}
