package jadex.benchmark;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.core.MjComponent;

/**
 *  Test plain MjComponent with included execution feature.
 */
public class ExecutableComponentCreationBenchmark	extends AbstractComponentCreationBenchmark 
{
	@Override
	protected String getComponentTypeName()
	{
		return "Executable component";
	}
	
	@Override
	protected IFuture<MjComponent>	createComponent(String name)
	{
		return new Future<>(IComponent.createComponent(MjComponent.class,
			() -> new MjComponent(null, new ComponentIdentifier(name))));
	}

	protected static Stream<Arguments> provideBenchmarkParams() {
	    return Stream.of(
	  	      Arguments.of(100000, false, false)
		      , Arguments.of(100000, false, true)	
	    );
	}
	
	@Override
	@ParameterizedTest
	@MethodSource("provideBenchmarkParams")
	public void runBenchmark(int num, boolean print, boolean parallel)
	{
		TIMEOUT	= 300000;
		super.runBenchmark(num, print, parallel);
	}
}
