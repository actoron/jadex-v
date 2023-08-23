package jadex.micro.tutorial;

import java.text.SimpleDateFormat;
import java.util.Date;

import jadex.mj.core.MjComponent;
import jadex.mj.feature.providedservice.impl.service.annotation.Service;
import jadex.mj.feature.providedservice.impl.service.annotation.ServiceComponent;
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
