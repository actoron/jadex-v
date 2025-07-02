package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
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
			Component	comp		= new Component(null, null, null);
			comp.init();
			comp.terminate().get();
		});
	}
	
	@Test
	void	benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() ->
		{
			IComponentHandle	comp	= IComponentManager.get().create(null).get();
//			Component	comp	= new Component();
			return () -> comp.terminate().get();			
		});
	}

	public static void	main(String[] args)
	{
		for(;;)
		{
			Component	comp		= new Component(null, null, null);
			comp.init();
			comp.terminate().get();
		}
	}
}
