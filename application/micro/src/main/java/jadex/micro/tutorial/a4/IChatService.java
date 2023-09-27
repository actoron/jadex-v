package jadex.micro.tutorial.a4;

import jadex.mj.feature.providedservice.annotation.Service;

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
