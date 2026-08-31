package jadex.micro.llmcall2;

import java.awt.image.RenderedImage;

import jadex.future.ITerminableIntermediateFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ILlmChatService2 
{
    public ITerminableIntermediateFuture<ChatFragment> chat(String systemprompt, String prompt, String schema, RenderedImage... images);
}
