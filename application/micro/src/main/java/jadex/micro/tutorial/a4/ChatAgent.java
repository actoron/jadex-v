package jadex.micro.tutorial.a4;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.annotation.OnService;

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
		MicroAgent.create(new ChatAgent());
		MicroAgent.create(new ChatAgent());
		MicroAgent.create(new ChatAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}