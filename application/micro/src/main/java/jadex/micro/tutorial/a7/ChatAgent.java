package jadex.micro.tutorial.a7;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jadex.common.SUtil;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.mj.core.IComponent;
import jadex.mj.core.annotation.OnStart;
import jadex.mj.feature.providedservice.annotation.Service;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.requiredservice.annotation.OnService;

/**
 *  Chat micro agent provides a basic chat service and publishes it as rest web service.
 */
@Agent
@Service
public class ChatAgent implements IChatService
{
	/** The underlying micro agent. */
	@Agent
	protected IComponent agent;
	
	@OnService
	protected Set<IChatService> chatservices = new HashSet<IChatService>();
	
	protected Set<SubscriptionIntermediateFuture<String>> subscribers = new HashSet<SubscriptionIntermediateFuture<String>>();
	
	@OnStart
	protected void onStart()
	{
		System.out.println("agent started: "+agent.getId().getLocalName());
	}
	
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void message(final String sender, final String text)
	{
		String txt = new SimpleDateFormat("hh:mm:ss").format(new Date())+" from: "+sender+" message: "+text;
		//System.out.println(txt);
		
		// Forward the chat message to the web ui
		//System.out.println("forward message to ui: "+subscribers.size());
		for(SubscriptionIntermediateFuture<String> subscriber: subscribers)
			subscriber.addIntermediateResultIfUndone(txt);
	}
	
	/**
	 *  Get the chat services.
	 *  @return The chat services.
	 */
	public Set<IChatService> getChatServices()
	{
		return chatservices;
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		MjMicroAgent.create(new ChatAgent());
		MjMicroAgent.create(new ChatAgent());
		MjMicroAgent.create(new ChatAgent());
		
		SUtil.sleep(10000);
	}
}