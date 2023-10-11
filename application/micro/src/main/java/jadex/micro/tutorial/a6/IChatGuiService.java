package jadex.micro.tutorial.a6;

import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.mj.feature.providedservice.annotation.Service;

@Service
public interface IChatGuiService 
{
	/**
	 *  Send a message to all chat services;
	 *  @param text The text.
	 */
	public void sendMessageToAll(String text);
	
	/**
	 *  Get the name of the chatter.
	 *  @return The name.
	 */
	public IFuture<String> getName();
	
	/**
	 *  Subscribe to chat.
	 *  @return Chat events.
	 */
	public ISubscriptionIntermediateFuture<String> subscribeToChat();
}
