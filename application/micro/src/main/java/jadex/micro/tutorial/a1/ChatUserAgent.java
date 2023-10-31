package jadex.micro.tutorial.a1;

import java.util.Collection;

import jadex.mj.core.IComponent;
import jadex.feature.execution.IExecutionFeature;
import jadex.mj.micro.MicroAgent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.model.annotation.OnStart;
import jadex.mj.requiredservice.IRequiredServiceFeature;

/**
 *  Chat micro agent gets chat services and sends a message.
 */
@Agent
//@RequiredServices({@RequiredService(name="chatservices", type=IChatService.class, scope=ServiceScope.PLATFORM)})
public class ChatUserAgent
{
	/** The underlying micro agent. */
	@Agent
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
		MicroAgent.create(new ChatProviderAgent());
		MicroAgent.create(new ChatProviderAgent());
		MicroAgent.create(new ChatProviderAgent());
		
		MicroAgent.create(new ChatUserAgent());
		
		IComponent.waitForLastComponentTerminated();
	}
}