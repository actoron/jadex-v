package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.INoCopyStep;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.execution.impl.FastLambda;
import jadex.future.Future;
import jadex.future.IFuture;

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
//		// Happens, because we create a component before the manager and use a future which triggers startNotifications() which uses isGuiThread!?
//		System.getLogger("jadex.benchmark.FastLambdaBenchmark");
		// Now "fixed" in SUtil.isGuiThread() by checking just the thread name instead of using SReflect.hasGui().
		
		try
		{
			FastLambda.KEEPALIVE	= true;	// Set to true for memory benchmarking
			
			BenchmarkHelper.benchmarkMemory(() -> 
			{
				// No handle is returned when creating fast lambdas, so we need to use a Future to get the handle.
				FastLambda<IComponent>	res	= new FastLambda<>((INoCopyStep<IComponent>)comp -> comp, null, null);
				Component.createComponent(res);
				IComponent	thecomp	= res.getResultFuture().get();
				return () -> thecomp.getComponentHandle().terminate().get();
			});
		}
		finally
		{
			FastLambda.KEEPALIVE	= false;	// Reset to false after memory benchmarking
		}
	}
	
	// Trigger bug
	public static void main(String[] args)
	{
		IFuture.DONE.then(x -> {});
	}
}
