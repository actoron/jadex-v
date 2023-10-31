package jadex.benchmark;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jadex.core.ComponentIdentifier;
import jadex.core.impl.Component;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Benchmark plain MjComponent with included execution feature.
 */
public class ExecutableComponentBenchmark	extends AbstractComponentBenchmark 
{
	@Override
	protected String getComponentTypeName()
	{
		return "Executable component";
	}
	
	@Override
	protected IFuture<ComponentIdentifier>	createComponent(String name)
	{
		return new Future<>(Component.createComponent(Component.class,
			() -> new Component(new ComponentIdentifier(name))).getId());
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
