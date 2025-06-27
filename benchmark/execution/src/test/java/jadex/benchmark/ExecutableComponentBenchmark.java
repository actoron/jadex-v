package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
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
		BenchmarkHelper.benchmarkTime(() -> 
		{
			Component	comp		= new Component(null);
			comp.init();
			comp.terminate().get();
		});
	}
	
	@Test
	void	benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() ->
		{
			IComponentHandle	comp	= Component.createComponent(Component.class, () -> new Component(null)).get();
//			Component	comp	= new Component();
			return () -> comp.terminate().get();			
		});
	}

	public static void	main(String[] args)
	{
		for(;;)
		{
//			Component.createComponent(Component.class, () -> new Component()).terminate().get();
			new Component(null).terminate().get();
		}
	}
}
