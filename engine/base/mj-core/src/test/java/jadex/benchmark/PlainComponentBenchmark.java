package jadex.benchmark;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.impl.MjComponent;

/**
 *  Benchmark MjComponent creation without any features.
 */
public class PlainComponentBenchmark	extends AbstractComponentBenchmark 
{
	@Override
	protected IFuture<ComponentIdentifier>	createComponent(String name)
	{
		return new Future<>(MjComponent.createComponent(MjComponent.class,
			() -> new MjComponent(new ComponentIdentifier(name))).getId());
	}

	protected static Stream<Arguments> provideBenchmarkParams() {
	    return Stream.of(
	  	      Arguments.of(100000, false, false),
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
