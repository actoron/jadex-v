package jadex.micro.tutorial.a1;

import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;

/**
 *  Chat micro agent provides a basic chat service as separate class. 
 */
@Agent
@ProvidedServices(@ProvidedService(type=IChatService.class, 
	implementation=@Implementation(ChatService.class)))
public class ChatProviderAgent
{
	/** The underlying micro agent. */
	@Agent
	protected MicroAgent agent;
	
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
		MicroAgent.create(new ChatProviderAgent());
	}
}