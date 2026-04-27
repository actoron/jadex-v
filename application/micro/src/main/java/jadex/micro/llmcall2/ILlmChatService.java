package jadex.micro.llmcall2;

import java.awt.image.RenderedImage;

import jadex.future.ITerminableIntermediateFuture;
import jadex.providedservice.annotation.Service;

/**
 *  Service interface for the LLM chat agent
 *  as alternative to runAsync() and component methods.
 */
@Service
public interface ILlmChatService
{
	public ITerminableIntermediateFuture<ChatFragment>	chat(String prompt, RenderedImage... images);
}
