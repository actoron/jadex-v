package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.micro.IMicroAgent;
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
		IExternalAccess s1 = BpmnProcess.create(new RBpmnProcess("jadex/bpmn/tutorial/S1_ProvidedServices.bpmn"));
		
		// todo: lambdas with required services
		/*IComponent.create(agent ->
		{
			agent.getFeature(IRequiredServiceFeature.class);
			return null;
		});*/
		
		IComponent.create(new IMicroAgent()
		{
			/*public void onStart(IComponent agent)
			{
				IAService aser = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IAService.class);
				String ret = aser.appendHello("hohoho").get();
			}*/
			
			@OnService
			public void onService(IAService aser, IComponent agent)
			{
				String ret = aser.appendHello("hohoho").get();
				System.out.println("terminating");
				//IComponent.terminate(s1.getId()).get();
				agent.terminate();
			}
		}).get();
		
		IComponent.waitForLastComponentTerminated();
		
		System.out.println("S1 finished");
	}
}
