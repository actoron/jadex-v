package jadex.micro.messagequeue;

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
		IComponentManager.get().create(new MessageQueueAgent());
		
		IComponentManager.get().create(new UserAgent());
		IComponentManager.get().create(new UserAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
