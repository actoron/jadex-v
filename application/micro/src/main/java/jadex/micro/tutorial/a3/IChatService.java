package jadex.micro.tutorial.a3;

import jadex.providedservice.annotation.Service;

/**
 *  The chat service interface.
 */
@Service
public interface IChatService
{
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void message(String sender, String text);
}
