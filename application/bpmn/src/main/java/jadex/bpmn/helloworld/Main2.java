package jadex.bpmn.helloworld;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponentManager;

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
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/helloworld/Empty.bpmn"));
		
		IComponentManager.get().waitForLastComponentTerminated();
		
		System.out.println("process terminated");
	}
}
