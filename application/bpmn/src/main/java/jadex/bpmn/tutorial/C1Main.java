package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponentManager;

/**
 *  Main for starting the example programmatically.
 */
public class C1Main
{
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) 
	{
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/C1_GlobalParameters.bpmn")
			.addArgument("customer", "Carl Customer")
			.addArgument("logins", 0)
		);
		
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/C1_GlobalParametersUpdate.bpmn")
			.addArgument("customer", "Carl Customer")
			.addArgument("logins", 0)
		);
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
