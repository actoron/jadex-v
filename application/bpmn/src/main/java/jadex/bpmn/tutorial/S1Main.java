package jadex.bpmn.tutorial;

import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.LambdaAgent;
import jadex.injection.annotation.Inject;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

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
		IComponentManager.get().create(new RBpmnProcess("jadex/bpmn/tutorial/S1_ProvidedServices.bpmn")).get();
		
		LambdaAgent.create(agent ->
		{
			//agent.getFeature(IExecutionFeature.class).waitForDelay(1000).get();
			IRequiredServiceFeature rsf = agent.getFeature(IRequiredServiceFeature.class);
			//IAService aser = rsf.getLocalService(IAService.class);
			IAService aser = rsf.searchService(new ServiceQuery<IAService>(IAService.class)).get();
			String ret = aser.appendHello("hohoho").get();
			System.out.println("terminating: "+ret+", "+agent.getId());
			agent.terminate();
		});
		
		IComponentManager.get().create(new Object()
		{
			@Inject
			public void onService(IAService aser, IComponent agent)
			{
				String ret = aser.appendHello("hohoho").get();
				System.out.println("terminating: "+ret+", "+agent.getId());
				//IComponent.terminate(s1.getId()).get();
				agent.terminate();
			}
		}).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
		
		System.out.println("S1 finished");
	}
}
