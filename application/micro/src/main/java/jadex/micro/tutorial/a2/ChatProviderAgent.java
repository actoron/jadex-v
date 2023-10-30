package jadex.micro.tutorial.a2;

import java.text.SimpleDateFormat;
import java.util.Date;

import jadex.mj.micro.MicroAgent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.model.annotation.OnStart;

/**
 *  Chat micro agent provides a basic chat service. 
 *  
 *  The agent provides the service implementation by itself. 
 */
@Agent
public class ChatProviderAgent implements IChatService
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
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void message(final String sender, final String text)
	{
		System.out.println(agent.getId()+" received at "
			+new SimpleDateFormat("hh:mm:ss").format(new Date())+" from: "+sender+" message: "+text);
	}
}