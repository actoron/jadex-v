package jadex.benchmark;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jadex.core.ComponentIdentifier;
import jadex.execution.LambdaAgent;
import jadex.future.IFuture;

/**
 *  Benchmark plain MjComponent with included execution feature.
 */
public class LambdaAgentBenchmark	extends AbstractComponentBenchmark 
{
	@Override
	protected String getComponentTypeName()
	{
		return "Lambda agent";
	}
	
	@Override
	protected IFuture<ComponentIdentifier>	createComponent(String name)
	{
		return LambdaAgent.create(comp ->
		{
			return comp.getId();
		}, new ComponentIdentifier(name));
	}

	protected static Stream<Arguments> provideBenchmarkParams() {
	    return Stream.of(
	  	      Arguments.of(10000, false, false),
		      Arguments.of(100000, false, true)	
	    );
	}
	
	@Override
	@ParameterizedTest
	@MethodSource("provideBenchmarkParams")
	public void runCreationBenchmark(int num, boolean print, boolean parallel)
	{
		super.runCreationBenchmark(num, print, parallel);
	}

	@Override
	@ParameterizedTest
	@MethodSource("provideBenchmarkParams")
	public void runThroughputBenchmark(int num, boolean print, boolean parallel)
	{
		super.runThroughputBenchmark(num, print, parallel);
	}
}
