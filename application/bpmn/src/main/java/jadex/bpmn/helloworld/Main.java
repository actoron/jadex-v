package jadex.bpmn.helloworld;

import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponent;
import jadex.core.IComponentManager;

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
		IComponentManager.get().create(new RBpmnProcess("jadex/bpmn/helloworld/HelloWorld.bpmn"));
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
