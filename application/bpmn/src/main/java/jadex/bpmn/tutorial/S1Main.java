package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.execution.IExecutionFeature;
import jadex.execution.LambdaAgent;
import jadex.micro.IMicroAgent;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.annotation.OnService;

/**
 *  Main for starting the example programmatically.
 */
public class S1Main
{
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) 
	{
		// todo: bpmn should not terminate after first call when it is a service component
		// in Jadex 4 we had keepalive flag at starting
		// Bpmn end check must be improved
		IExternalAccess s1 = BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/S1_ProvidedServices.bpmn"));
		
		/*LambdaAgent.create(agent ->
		{
			agent.getFeature(IExecutionFeature.class).waitForDelay(1000).get();
			
			IRequiredServiceFeature rsf = agent.getFeature(IRequiredServiceFeature.class);
			IAService aser = rsf.getLocalService(IAService.class);
			String ret = aser.appendHello("hohoho").get();
			System.out.println("terminating: "+agent.getId());
			agent.terminate();
		});*/
		
		IComponent.create(new IMicroAgent()
		{
			//public void onStart(IComponent agent)
			//{
			//	IAService aser = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IAService.class);
			//	String ret = aser.appendHello("hohoho").get();
			//}
			
			@OnService
			public void onService(IAService aser, IComponent agent)
			{
				String ret = aser.appendHello("hohoho").get();
				System.out.println("terminating: "+agent.getId());
				//IComponent.terminate(s1.getId()).get();
				agent.terminate();
			}
		}).get();
		
		IComponent.waitForLastComponentTerminated();
		
		System.out.println("S1 finished");
	}
}
