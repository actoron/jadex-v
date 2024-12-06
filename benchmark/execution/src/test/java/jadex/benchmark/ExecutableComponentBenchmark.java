package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
//		double	pct	= BenchmarkHelper.benchmarkTime(() -> Component.createComponent(Component.class, () -> new Component()).terminate().get());
		double	pct	= BenchmarkHelper.benchmarkTime(() -> new Component().terminate().get());
		assertTrue(pct<20);	// Fail when more than 20% worse
	}
	
	@Test
	void	benchmarkMemory()
	{
		double	pct	= BenchmarkHelper.benchmarkMemory(() ->
		{
			Component	comp	= Component.createComponent(Component.class, () -> new Component());
//			Component	comp	= new Component();
			return () -> comp.terminate().get();			
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}
}
