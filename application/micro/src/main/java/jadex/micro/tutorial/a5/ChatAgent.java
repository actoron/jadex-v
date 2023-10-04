package jadex.micro.tutorial.a5;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jadex.common.SUtil;
import jadex.mj.core.IComponent;
import jadex.mj.core.annotation.OnEnd;
import jadex.mj.core.annotation.OnStart;
import jadex.mj.feature.providedservice.annotation.Service;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.publishservice.publish.annotation.Publish;
import jadex.mj.requiredservice.annotation.OnService;

/**
 *  Chat micro agent provides a basic chat service. 
 */
@Agent
@Service
@Publish(publishid="http://localhost:8081/chat")
public class ChatAgent implements IChatService
{
	/** The underlying micro agent. */
	@Agent
	protected IComponent agent;
	
	@OnService
	protected Set<IChatService> chatservices = new HashSet<IChatService>();
	
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void message(final String sender, final String text)
	{
		String txt = agent.getId()+" received at "+new SimpleDateFormat("hh:mm:ss").format(new Date())+" from: "+sender+" message: "+text;
		System.out.println(txt);
		//gui.addMessage(txt);
	}
	
	@OnStart
	protected void onStart()
	{
		System.out.println("agent started: "+agent.getId());
		
		//this.gui = new ChatGui(agent.getExternalAccess());
	}
	
	@OnEnd
	protected void end()
	{
		System.out.println("on end called: "+agent.getId());
		//this.gui.dispose();
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
		//MjMicroAgent.create(new ChatAgent());
		//MjMicroAgent.create(new ChatAgent());
		
		SUtil.sleep(10000);
	}
}