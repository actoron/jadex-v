package jadex.micro.tutorial.a1;

import jadex.mj.core.MjComponent;
import jadex.mj.core.annotation.OnStart;
import jadex.mj.feature.providedservice.annotation.Implementation;
import jadex.mj.feature.providedservice.annotation.ProvidedService;
import jadex.mj.feature.providedservice.annotation.ProvidedServices;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.micro.annotation.Description;

/**
 *  Chat micro agent provides a basic chat service. 
 */
@Agent
@ProvidedServices(@ProvidedService(type=IChatService.class, 
	implementation=@Implementation(ChatService.class)))
public class ChatProviderAgent
{
	/** The underlying micro agent. */
	@Agent
	protected MjMicroAgent agent;
	
	@OnStart
	protected void onStart()
	{
		System.out.println("agent started: "+agent.getId());
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		MjMicroAgent.create(new ChatProviderAgent());
	}
}