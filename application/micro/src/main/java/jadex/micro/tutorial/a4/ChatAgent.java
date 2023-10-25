package jadex.micro.tutorial.a4;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jadex.common.SUtil;
import jadex.mj.core.IComponent;
import jadex.mj.feature.providedservice.annotation.Service;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.model.annotation.OnEnd;
import jadex.mj.model.annotation.OnStart;
import jadex.mj.requiredservice.annotation.OnService;

/**
 *  Chat micro agent provides a basic chat service. 
 *  
 *  This example shows how a service query can be used on a collection variable.
 *  The var 'chatservices' is kept up to date with available services.
 */
@Agent
@Service
public class ChatAgent implements IChatService
{
	/** The underlying micro agent. */
	@Agent
	protected IComponent agent;
	
	protected ChatGui gui;
	
	@OnService
	protected Set<IChatService> chatservices = new HashSet<IChatService>();
	
	@OnStart
	protected void onStart()
	{
		System.out.println("agent started: "+agent.getId());
		
		this.gui = new ChatGui(agent.getExternalAccess());
	}
	
	@OnEnd
	protected void end()
	{
		//System.out.println("on end called: "+agent.getId());
		this.gui.dispose();
	}
	
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void message(final String sender, final String text)
	{
		String txt = agent.getId()+" received at "+new SimpleDateFormat("hh:mm:ss").format(new Date())+" from: "+sender+" message: "+text;
		gui.addMessage(txt);
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
		
		IComponent.waitForLastComponentTerminated();
	}
}