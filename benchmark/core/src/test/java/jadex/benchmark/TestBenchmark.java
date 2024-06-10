package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.core.impl.Component;

public class TestBenchmark
{
//	@Test
//	void testBenchmark()
//	{
//		BenchmarkHelper.benchmarkTwoStage(() ->
//		{
//			IComponent	comp	= new Component();
//			return () -> comp.terminate().get();
//		});
//	}
	
	@Test
	void	benchmarkTime()
	{
		double	pct	= BenchmarkHelper.benchmarkTime(() -> new Component().terminate().get());
		assertTrue(pct<20);	// Fail when more than 20% worse
	}
}
