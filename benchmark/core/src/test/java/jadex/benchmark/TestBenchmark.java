package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.impl.Component;

public class TestBenchmark
{
	@Test
	void testBenchmark()
	{
		BenchmarkHelper.benchmarkTwoStage(() ->
		{
			IComponent	comp	= new Component();
			return () -> comp.terminate().get();
		});
	}
	
	@Test
	void	benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() -> new Component().terminate().get());
	}
}
