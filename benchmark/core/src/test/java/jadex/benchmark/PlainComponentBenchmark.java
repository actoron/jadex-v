package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;

public class PlainComponentBenchmark
{
	@Test
	void	benchmarkMemory()
	{
//		IComponentManager.get().setComponentIdNumberMode(true);
		BenchmarkHelper.benchmarkMemory(() ->
		{
//			Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
			IComponentHandle	comp	= IComponentManager.get().create(null).get();
			return () -> comp.terminate().get();
		});
	}
	
	@Test
	void	benchmarkTime()
	{
//		IComponentManager.get().setComponentIdNumberMode(true);
		BenchmarkHelper.benchmarkTime(() ->
		{
			IComponentManager.get().create(null).get().terminate().get();
		});
	}

	public static void	main(String[] args)
	{
		for(;;)
		{
			IComponentManager.get().create(null).get().terminate().get();
		}
	}

//	@Test
//	void	benchmarkGetLoggerTime()
//	{
//		Component	comp	= new Component(this);
//		BenchmarkHelper.benchmarkTime(() ->
//		{
//			if(comp.getLogger().isLoggable(java.lang.System.Logger.Level.DEBUG))
//				comp.getLogger().log(java.lang.System.Logger.Level.DEBUG, "test");
//		});
//	}
}
