package jadex.benchmark;


import org.junit.jupiter.api.Test;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.future.Future;

/**
 *  Benchmark creation and killing of bpmn processes.
 */
public class BpmnProcessBenchmark 
{
	@Test
	void	benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			Future<ComponentIdentifier>	ret	= new Future<>();
			RBpmnProcess pojo = new RBpmnProcess("jadex/benchmark/Benchmark.bpmn").declareResult("result");
			
			pojo.subscribeToResults().next(res ->
			{
				ret.setResultIfUndone((ComponentIdentifier)res.value());
			}).catchEx(ret);
			
			IComponentHandle	agent	= BpmnProcess.create(pojo);
			ret.get();
			return () -> agent.terminate().get();
		});
	}
	
	@Test
	void	benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<ComponentIdentifier>	ret	= new Future<>();
			RBpmnProcess pojo = new RBpmnProcess("jadex/benchmark/Benchmark.bpmn").declareResult("result");
			
			pojo.subscribeToResults().next(res ->
			{
				ret.setResultIfUndone((ComponentIdentifier)res.value());
			}).catchEx(ret);
			
			IComponentHandle	agent	= BpmnProcess.create(pojo);
			ret.get();
			agent.terminate().get();
		});
	}
}


