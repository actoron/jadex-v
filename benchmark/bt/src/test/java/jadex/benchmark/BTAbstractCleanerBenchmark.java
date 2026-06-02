package jadex.benchmark;

import java.lang.System.Logger.Level;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.management.HotSpotDiagnosticMXBean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;

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
		env.getPojoHandle(CleanerworldEnvironment.class).createWorld().get();
		envid = Environment.add(env.getPojoHandle(CleanerworldEnvironment.class));
		
		lsystem = IComponentManager.get().getFeature(ILoggingFeature.class).getSystemLoggingLevel();
		lapp = IComponentManager.get().getFeature(ILoggingFeature.class).getAppLogginglevel();
		IComponentManager.get().getFeature(ILoggingFeature.class).setSystemLoggingLevel(Level.ERROR);
		IComponentManager.get().getFeature(ILoggingFeature.class).setAppLoggingLevel(Level.WARNING);
	}
	
	@AfterEach
	public void afterEach()
	{
		env.terminate().get();
		Environment.remove(envid);
		IComponentManager.get().getFeature(ILoggingFeature.class).setSystemLoggingLevel(lsystem);
		IComponentManager.get().getFeature(ILoggingFeature.class).setAppLoggingLevel(lapp);

		String tmpDir = System.getenv("TEST_TMPDIR");
		if (tmpDir == null) 
			tmpDir = "/tmp";
		String file = tmpDir + "/heap-" + System.currentTimeMillis() + ".hprof";

		dumpHeap(file, true);
	}

	/*@AfterAll
	static void afterAll() throws Exception 
	{
		String file = "/tmp/heap-" + System.currentTimeMillis() + ".hprof";
		dumpHeap(file, true);
	}*/

	public static void dumpHeap(String file, boolean live)
	{
		try
		{
			Path path = Paths.get(file).toAbsolutePath();
			Files.createDirectories(path.getParent());

			HotSpotDiagnosticMXBean mxBean =
				ManagementFactory.newPlatformMXBeanProxy(
					ManagementFactory.getPlatformMBeanServer(),
					"com.sun.management:type=HotSpotDiagnostic",
					HotSpotDiagnosticMXBean.class);

			mxBean.dumpHeap(path.toString(), live);

			System.out.println("Heap dumped to: " + path);
		}
		catch (Exception e)
		{
			System.out.println("Failed to dump heap: " + e.getMessage());
		}
    }
}
