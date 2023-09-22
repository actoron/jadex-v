package jadex.micro.tutorial.a1;

import java.util.Collection;

import jadex.common.SUtil;
import jadex.mj.core.IComponent;
import jadex.mj.core.annotation.OnStart;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.requiredservice.IMjRequiredServiceFeature;

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
		IMjRequiredServiceFeature rsf = agent.getFeature(IMjRequiredServiceFeature.class);
		Collection<IChatService> chatservices = rsf.getLocalServices(IChatService.class);
		System.out.println("Chat user found chat services: "+chatservices.size());
		for(IChatService cs: chatservices)
		{
			cs.message(agent.getId().toString(), "Hello");
		}
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		MjMicroAgent.create(new ChatProviderAgent());
		MjMicroAgent.create(new ChatProviderAgent());
		MjMicroAgent.create(new ChatProviderAgent());
		
		SUtil.sleep(1000);
		
		MjMicroAgent.create(new ChatUserAgent());
	}
}