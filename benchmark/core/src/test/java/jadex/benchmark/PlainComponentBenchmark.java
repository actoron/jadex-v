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
}
