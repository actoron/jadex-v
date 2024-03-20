package jadex.micro.messagequeue;

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
		IComponent.create(new MessageQueueAgent());
		
		IComponent.create(new UserAgent());
		IComponent.create(new UserAgent());
		
		IComponent.waitForLastComponentTerminated();
	}
}
