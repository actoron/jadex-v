package jadex.benchmark;


import org.junit.jupiter.api.Test;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingConsumer;
import jadex.future.Future;
import jadex.result.IResultFeature;
import jadex.result.impl.ResultFeature;

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
			IComponentHandle	agent	= BpmnProcess.create(pojo).get();
			agent.subscribeToResults().next(res ->
			{
				ret.setResultIfUndone((ComponentIdentifier)res.value());
			}).catchEx(ret);

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
			IComponentHandle	agent	= BpmnProcess.create(pojo).get();
			// Use internal result subscription to not measure result decoupling overhead.
			IThrowingConsumer<IComponent>	step	= comp -> ((ResultFeature)comp.getFeature(IResultFeature.class))
				.subscribeToResults().next(res -> ret.setResultIfUndone((ComponentIdentifier)res.value())).catchEx(ret);
			agent.scheduleStep(step);			
			ret.get();
			agent.terminate().get();
		});
	}
	
	public static void main(String[] args)
	{
		new BpmnProcessBenchmark().benchmarkMemory();
	}
}


