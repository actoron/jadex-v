package jadex.micro.llmcall2;

import java.awt.image.RenderedImage;

import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.providedservice.annotation.Service;

/**
 *  Service interface for the LLM chat agent
 *  as alternative to runAsync() and component methods.
 */
@Service
public interface ILlmChatService
{
	/**
	 *  Send a prompt to the agent.
	 *  This can be used to send an initial prompt at the start or follow-up prompts later on.
	 */
	public ITerminableIntermediateFuture<ChatFragment>	chat(String prompt, RenderedImage... images);

	/**
	 *  Get the total token count of the last completed chat interaction, including all tokens
	 *  (complete history of user, assistant, tool tokens).
	 */
	public IFuture<Integer>	getTotalTokenCount();
}
