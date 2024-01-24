package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponent;

/**
 *  Main for starting the example programmatically.
 */
public class E4Main
{
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) 
	{
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/E4_AsynchronousResultTask.bpmn"));
		
		IComponent.waitForLastComponentTerminated();
		
		System.out.println("process terminated");
	}
}
