package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.execution.LambdaAgent;
import jadex.execution.impl.FastLambda;

/**
 *  Benchmark lifecycle-optimized lambda agents.
 */
public class FastLambdaBenchmark
{
	@Test
	void	benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			IThrowingFunction<IComponent, ComponentIdentifier>	body	= comp ->{return comp.getId();};
			@SuppressWarnings("unchecked")
			FastLambda<ComponentIdentifier> comp = Component.createComponent(FastLambda.class, () -> new FastLambda<>(body, null, false));
			return () -> comp.terminate().get();
		});
	}
	
	@Test
	void	benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			LambdaAgent.run(comp ->{return comp.getId();}).get();
		});
	}

	public static void main(String[] args)
	{
		for(;;)
		{
			LambdaAgent.run(comp ->{return comp.getId();}).get();
		}
	}
}
