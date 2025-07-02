package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponentManager;

/**
 *  Main for starting the example programmatically.
 */
public class C2Main
{
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) 
	{
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/C2_LocalParameters.bpmn"));

		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/C2_LocalParametersBoth.bpmn"));

		IComponentManager.get().waitForLastComponentTerminated();
	}
}
