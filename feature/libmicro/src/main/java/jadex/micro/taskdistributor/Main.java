package jadex.micro.taskdistributor;

import jadex.core.IComponentManager;

/**
 *  Main for starting a test scenario.
 */
public class Main 
{
	public static void main(String[] args) 
	{
		IComponentManager.get().create(new TaskDistributorAgent<String, String>());
		
		IComponentManager.get().create(new TaskCreatorAgent(1));
		IComponentManager.get().create(new TaskCreatorAgent(1));
		IComponentManager.get().create(new TaskCreatorAgent(3));
		
		IComponentManager.get().create(new TaskWorkerAgent());
		IComponentManager.get().create(new TaskWorkerAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
