package jadex.bpmn.examples.helloworld;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponent;

/**
 *  Main for starting the example programmatically.
 */
public class Main2 
{
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/examples/helloworld/Empty.bpmn"));
		
		IComponent.waitForLastComponentTerminated();
		
		System.out.println("process terminated");
	}
}
