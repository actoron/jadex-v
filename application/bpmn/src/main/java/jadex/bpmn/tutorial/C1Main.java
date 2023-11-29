package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;

/**
 *  Main for starting the example programmatically.
 */
public class C1Main
{
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/C1_GlobalParameters.bpmn")
			.addArgument("customer", "Carl Customer")
			.addArgument("logins", 0)
		);
	}
}
