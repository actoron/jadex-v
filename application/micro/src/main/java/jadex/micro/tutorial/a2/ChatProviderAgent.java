package jadex.micro.tutorial.a2;

import java.text.SimpleDateFormat;
import java.util.Date;

import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

/**
 *  Chat micro agent provides a basic chat service. 
 *  
 *  The agent provides the service implementation by itself. 
 */
public class ChatProviderAgent implements IChatService
{
	/** The underlying micro agent. */
	@Inject
	protected IComponent agent;
	
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