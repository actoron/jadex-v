package jadex.benchmark;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jadex.bdi.runtime.BDIBaseAgent;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.model.annotation.OnStart;

public class PojoBDIBenchmark	extends AbstractComponentBenchmark
{
	@Override
	protected String getComponentTypeName()
	{
		return "Pojo BDI agent";
	}
	
	@Override
	protected IFuture<ComponentIdentifier> createComponent(String name)
	{
		Future<ComponentIdentifier>	cid	= new Future<>();
		IComponent.create(new BDIBaseAgent()
		{
			@OnStart
			public void start()
			{
				cid.setResult(__agent.getId());
			}
		}, new ComponentIdentifier(name));
		return cid;
	}
	
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
		TIMEOUT	= 300000;
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
