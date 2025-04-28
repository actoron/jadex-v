package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.impl.Component;

/**
 *  Benchmark plain MjComponent with included execution feature.
 */
public class ExecutableComponentBenchmark 
{
	@Test
	void	benchmarkTime()
	{
//		BenchmarkHelper.benchmarkTime(() -> Component.createComponent(Component.class, () -> new Component()).terminate().get());
		BenchmarkHelper.benchmarkTime(() -> new Component(this).terminate().get());
	}
	
	@Test
	void	benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() ->
		{
			Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
//			Component	comp	= new Component();
			return () -> comp.terminate().get();			
		});
	}
}
