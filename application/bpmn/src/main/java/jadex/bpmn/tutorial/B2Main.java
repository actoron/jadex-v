package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;

/**
 *  Main for starting the example programmatically.
 */
public class B2Main
{
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) 
	{
		BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/B2_Sequence.bpmn"));
	}
}
