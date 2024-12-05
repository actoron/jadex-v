package jadex.micro.tutorial.a2;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.requiredservice.annotation.OnService;

/**
 *  Chat micro agent gets chat services and sends a message.
 */
@Agent
public class ChatUserAgent
{
	/** The underlying micro agent. */
	@Agent
	protected IComponent agent;

	@OnService
	public void chatServiceFound(IChatService cs)
	{
		cs.message(agent.getId().toString(), "Hello");
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		MicroAgent.create(new ChatUserAgent());
		
		MicroAgent.create(new ChatProviderAgent());
		MicroAgent.create(new ChatProviderAgent());
		MicroAgent.create(new ChatProviderAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}