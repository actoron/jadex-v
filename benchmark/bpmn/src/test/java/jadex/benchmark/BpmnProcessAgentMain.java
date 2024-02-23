package jadex.benchmark;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponent;

public class BpmnProcessAgentMain 
{
	public static void main(String[] args) 
	{
		RBpmnProcess pojo = new RBpmnProcess("jadex/benchmark/Benchmark.bpmn").declareResult("result");
		
		pojo.subscribeToResults().next(res ->
		{
			System.out.println("received result: "+res.name()+" "+res.value());
		});
		
		BpmnProcess.create(pojo);
		
		IComponent.waitForLastComponentTerminated();
	}
}
