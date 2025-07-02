package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponentManager;

/**
 *  Main for starting the example programmatically.
 */
public class C3Main
{
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) 
	{
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/C3_ParameterScopes.bpmn"));

		//BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/C3_Scopes.bpmn"));
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
