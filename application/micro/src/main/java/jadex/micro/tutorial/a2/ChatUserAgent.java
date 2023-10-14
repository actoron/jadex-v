package jadex.micro.tutorial.a2;

import jadex.common.SUtil;
import jadex.mj.core.IComponent;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.requiredservice.annotation.OnService;

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
		MjMicroAgent.create(new ChatUserAgent());
		
		MjMicroAgent.create(new ChatProviderAgent());
		MjMicroAgent.create(new ChatProviderAgent());
		MjMicroAgent.create(new ChatProviderAgent());
		
		IComponent.waitForLastComponentTerminated();
	}
}