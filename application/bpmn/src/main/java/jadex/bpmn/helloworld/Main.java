package jadex.bpmn.helloworld;

import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponent;

/**
 *  Main for starting the example programmatically.
 */
public class Main 
{
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		IComponent.create(new RBpmnProcess("jadex/bpmn/helloworld/HelloWorld.bpmn"));
		IComponent.waitForLastComponentTerminated();
	}
}
