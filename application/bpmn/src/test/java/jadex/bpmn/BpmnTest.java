package jadex.bpmn;

import org.junit.jupiter.api.Test;

import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponentManager;

/**
 *  Test that a simple process can be run
 */
public class BpmnTest
{
	@Test
	public void	startProcess()
	{
		IComponentManager.get().create(new RBpmnProcess("jadex/bpmn/tutorial/B4_Choice.bpmn")).get(10000);
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
