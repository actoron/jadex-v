package jadex.benchmark;


import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jadex.benchmark.AbstractComponentBenchmark;
import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.ComponentIdentifier;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Benchmark creation and killing of micro agents.
 */
public class BpmnProcessAgentBenchmark	extends AbstractComponentBenchmark 
{
	@Override
	protected String getComponentTypeName()
	{
		return "BPMN Process";
	}
	
	@Override
	protected IFuture<ComponentIdentifier>	createComponent(String name)
	{
		Future<ComponentIdentifier>	ret	= new Future<>();
		RBpmnProcess pojo = new RBpmnProcess("jadex/benchmark/Benchmark.bpmn").declareResult("result");
		
		pojo.subscribeToResults().next(res ->
		{
			//System.out.println("received result: "+res.name()+" "+res.value());
			ret.setResult((ComponentIdentifier)res.value());
		}).catchEx(ret);
		
		BpmnProcess.create(pojo, new ComponentIdentifier(name));
		//System.out.println(name);
		return ret;
	}

	protected static Stream<Arguments> provideBenchmarkParams() 
	{
	    return Stream.of
	    (
	  	      Arguments.of(1000, false, false),
		      Arguments.of(1000, false, true)
	    );
	}
	
	@Override
	@ParameterizedTest
	@MethodSource("provideBenchmarkParams")
	public void runCreationBenchmark(int num, boolean print, boolean parallel)
	{
		System.out.println("runCreationBenchmark: "+num+" "+print+" "+parallel);
		super.runCreationBenchmark(num, print, parallel);
		System.out.println("runCreationBenchmark end");
	}

	@Override
	@ParameterizedTest
	@MethodSource("provideBenchmarkParams")
	public void runThroughputBenchmark(int num, boolean print, boolean parallel)
	{
		TIMEOUT	= 300000;
		System.out.println("runThroughputBenchmark: "+num+" "+print+" "+parallel);
		super.runThroughputBenchmark(num, print, parallel);
		System.out.println("runThroughputBenchmark end");
	}
}


