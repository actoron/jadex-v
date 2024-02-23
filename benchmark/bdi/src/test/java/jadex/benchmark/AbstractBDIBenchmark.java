package jadex.benchmark
;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jadex.bdi.runtime.BDICreationInfo;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public abstract class AbstractBDIBenchmark	extends AbstractComponentBenchmark
{
	// Corresponding BDI agent is in testFixtures so class is not loaded by JUnit to check for tests.
	@Override
	protected IFuture<ComponentIdentifier> createComponent(String name)
	{
		Future<ComponentIdentifier>	cid	= new Future<>();
		IComponent.create(new BDICreationInfo()
				.setClassname(getClassname())
				.addArgument("cid", cid),
			new ComponentIdentifier(name));
		return cid;
	}
	
	protected abstract String getClassname();

	protected static Stream<Arguments> provideBenchmarkParams() {
	    return Stream.of(
	  	      Arguments.of(10000, false, false),
		      Arguments.of(10000, false, true)
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
