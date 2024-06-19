package jadex.benchmark;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.ComponentIdentifier;
import jadex.core.IExternalAccess;
import jadex.future.Future;

/**
 *  Benchmark creation and killing of micro agents.
 */
public class BpmnProcessAgentBenchmark 
{
	@Test
	void	benchmarkTime()
	{
		double pct	= BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<ComponentIdentifier>	ret	= new Future<>();
			RBpmnProcess pojo = new RBpmnProcess("jadex/benchmark/Benchmark.bpmn").declareResult("result");
			
			pojo.subscribeToResults().next(res ->
			{
//				System.out.println("received result: "+res.name()+" "+res.value());
				ret.setResultIfUndone((ComponentIdentifier)res.value());
			}).catchEx(ret);
			
			IExternalAccess	agent	= BpmnProcess.create(pojo);
			ret.get();
			agent.terminate().get();
		});
		assertTrue(pct<20);	// Fail when more than 20% worse
	}
}


