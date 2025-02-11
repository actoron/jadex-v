package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
		double pct	= BenchmarkHelper.benchmarkMemory(() -> 
		{
			IThrowingFunction<IComponent, ComponentIdentifier>	body	= comp ->{return comp.getId();};
			@SuppressWarnings("unchecked")
			FastLambda<ComponentIdentifier> comp = Component.createComponent(FastLambda.class, () -> new FastLambda<>(body, null, false));
			return () -> comp.terminate().get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}
	
	@Test
	void	benchmarkTime()
	{
		double pct	= BenchmarkHelper.benchmarkTime(() -> 
		{
			LambdaAgent.run(comp ->{return comp.getId();}).get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}
}
