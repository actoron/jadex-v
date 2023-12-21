package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.common.SUtil;
import jadex.core.IComponent;

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
		
		SUtil.sleep(1000);
		
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/S2_RequiredServices.bpmn"));
		
		IComponent.waitForLastComponentTerminated();
		
		System.out.println("process terminated");
	}
}
