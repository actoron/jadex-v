package jadex.micro.taskdistributor;

import jadex.core.IComponent;

/**
 *  Main for starting a test scenario.
 */
public class Main 
{
	public static void main(String[] args) 
	{
		IComponent.create(new TaskDistributorAgent<String, String>());
		
		IComponent.create(new TaskCreatorAgent(1));
		IComponent.create(new TaskCreatorAgent(1));
		IComponent.create(new TaskCreatorAgent(3));
		
		IComponent.create(new TaskWorkerAgent());
		IComponent.create(new TaskWorkerAgent());
	}
}
