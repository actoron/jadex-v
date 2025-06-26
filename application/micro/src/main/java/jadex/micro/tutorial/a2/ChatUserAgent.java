package jadex.micro.tutorial.a2;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.injection.annotation.Inject;

/**
 *  Chat micro agent gets chat services and sends a message.
 */
public class ChatUserAgent
{
	/** The underlying micro agent. */
	@Inject
	protected IComponent agent;

	@Inject
	public void chatServiceFound(IChatService cs)
	{
		cs.message(agent.getId().toString(), "Hello");
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		IComponentManager.get().create(new ChatUserAgent());
		
		IComponentManager.get().create(new ChatProviderAgent());
		IComponentManager.get().create(new ChatProviderAgent());
		IComponentManager.get().create(new ChatProviderAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}