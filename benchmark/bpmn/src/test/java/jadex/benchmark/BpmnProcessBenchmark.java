package jadex.benchmark;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.ComponentIdentifier;
import jadex.core.IExternalAccess;
import jadex.future.Future;

/**
 *  Benchmark creation and killing of bpmn processes.
 */
public class BpmnProcessBenchmark 
{
	@Test
	void	benchmarkMemory()
	{
		double pct	= BenchmarkHelper.benchmarkMemory(() -> 
		{
			Future<ComponentIdentifier>	ret	= new Future<>();
			RBpmnProcess pojo = new RBpmnProcess("jadex/benchmark/Benchmark.bpmn").declareResult("result");
			
			pojo.subscribeToResults().next(res ->
			{
				ret.setResultIfUndone((ComponentIdentifier)res.value());
			}).catchEx(ret);
			
			IExternalAccess	agent	= BpmnProcess.create(pojo);
			ret.get();
			return () -> agent.terminate().get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}
	
	@Test
	void	benchmarkTime()
	{
		double pct	= BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<ComponentIdentifier>	ret	= new Future<>();
			RBpmnProcess pojo = new RBpmnProcess("jadex/benchmark/Benchmark.bpmn").declareResult("result");
			
			pojo.subscribeToResults().next(res ->
			{
				ret.setResultIfUndone((ComponentIdentifier)res.value());
			}).catchEx(ret);
			
			IExternalAccess	agent	= BpmnProcess.create(pojo);
			ret.get();
			agent.terminate().get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}
}


