package jadex.micro.tutorial.a1;

import java.text.SimpleDateFormat;
import java.util.Date;

import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.providedservice.annotation.Service;

/**
 *  Chat service implementation.
 */
@Service
public class ChatService implements IChatService
{
	//-------- attributes --------
	
	/** The agent. */
	@Inject
	protected IComponent agent;

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
