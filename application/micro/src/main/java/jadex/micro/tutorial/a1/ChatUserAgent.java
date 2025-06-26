package jadex.micro.tutorial.a1;

import java.util.Collection;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Chat micro agent gets chat services and sends a message.
 */
public class ChatUserAgent
{
	/** The underlying micro agent. */
	@Inject
	protected IComponent agent;

	/**
	 *  Execute the functional body of the agent.
	 *  Is only called once.
	 */
	@OnStart
	public void executeBody()
	{
		while(true)
		{
			IRequiredServiceFeature rsf = agent.getFeature(IRequiredServiceFeature.class);
			Collection<IChatService> chatservices = rsf.getLocalServices(IChatService.class);
			System.out.println("Chat user found chat services: "+chatservices.size());
			for(IChatService cs: chatservices)
			{
				cs.message(agent.getId().toString(), "Hello");
			}
			
			agent.getFeature(IExecutionFeature.class).waitForDelay(1000).get();
		}
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		IComponentManager.get().create(new ChatProviderAgent());
		IComponentManager.get().create(new ChatProviderAgent());
		IComponentManager.get().create(new ChatProviderAgent());
		
		IComponentManager.get().create(new ChatUserAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}