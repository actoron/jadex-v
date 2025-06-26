package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponentManager;

/**
 *  Main for starting the example programmatically.
 */
public class S2Main
{
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) 
	{
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/S1_ProvidedServices.bpmn"));
		
//		SUtil.sleep(1000);
		
//		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/S2_RequiredServices.bpmn"));
//		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/S2_RequiredServices2.bpmn"));
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/S2_RequiredServices3.bpmn"));
		
		IComponentManager.get().waitForLastComponentTerminated();
		
		System.out.println("process terminated");
	}
}
