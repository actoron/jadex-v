package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.execution.impl.FastLambda;
import jadex.future.Future;

/**
 *  Benchmark lifecycle-optimized lambda agents.
 */
public class FastLambdaBenchmark
{
	@Test
	void	benchmarkMemory()
	{
//		// Hack!!! We need to ensure that the logger is initialized before we start the benchmark.
//		// Otherwise, SReflect.hasGui() will hang, because swing tries to get a logger on init.
//		// Happens, because we create a component before the manager and use a future which triggers startNotifications() wich uses isGuiThread!?
//		System.getLogger("jadex.benchmark.FastLambdaBenchmark");
		
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			IThrowingFunction<IComponent, ComponentIdentifier>	body	= comp ->{return comp.getId();};
			// No handle is returned when creating fast lambdas, so we need to use a Future to get the handle.
			Future<IComponentHandle>	comp	= new Future<>();
			Component.createComponent(FastLambda.class, () -> new FastLambda<>(body, null, false)
			{{
				comp.setResult(this.getComponentHandle());
			}});
			IComponentHandle	handle	= comp.get();
			return () -> handle.terminate().get();
		});
	}
}
