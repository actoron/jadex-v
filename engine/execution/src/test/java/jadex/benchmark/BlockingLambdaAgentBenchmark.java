package jadex.benchmark;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.execution.LambdaAgent;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Benchmark plain MjComponent with included execution feature.
 */
public class BlockingLambdaAgentBenchmark	extends AbstractComponentBenchmark 
{
	@Override
	protected String getComponentTypeName()
	{
		return "Blocking lambda agent";
	}
	
	@Override
	protected IFuture<ComponentIdentifier>	createComponent(String name)
	{
		Future<ComponentIdentifier>	ret	= new Future<>();
		LambdaAgent.create(comp ->
		{
			ret.setResult(comp.getId());
			new Future<Void>().get();
		}, new ComponentIdentifier(name));
		return ret;
	}

	protected static Stream<Arguments> provideBenchmarkParams() {
	    return Stream.of(
	  	      Arguments.of(SUtil.isVirtualExecutor()  ? 10000 : 1000, false, false),
		      Arguments.of(SUtil.isVirtualExecutor()  ? 100000 : 10000, false, true)	
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
