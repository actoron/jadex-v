package jadex.micro.tutorial.a1;

import java.text.SimpleDateFormat;
import java.util.Date;

import jadex.mj.core.impl.MjComponent;
import jadex.mj.feature.providedservice.annotation.Service;
import jadex.mj.feature.providedservice.annotation.ServiceComponent;
import jadex.mj.micro.MjMicroAgent;

/**
 *  Chat service implementation.
 */
@Service
public class ChatService implements IChatService
{
	//-------- attributes --------
	
	/** The agent. */
	@ServiceComponent
	protected MjMicroAgent agent;

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
