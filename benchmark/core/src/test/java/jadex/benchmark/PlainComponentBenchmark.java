package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.impl.Component;

public class PlainComponentBenchmark
{
	@Test
	void	benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() ->
		{
			Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
			return () -> comp.terminate().get();
		});
	}
	
	@Test
	void	benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() ->
		{
			Component.createComponent(Component.class, () -> new Component(this)).terminate().get();
		});
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
