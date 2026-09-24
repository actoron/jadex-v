package jadex.llm.twentyquestions;

import dev.langchain4j.agent.tool.Tool;
import jadex.core.impl.IDaemonComponent;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ITwentyQuestionsBareService	extends IDaemonComponent
{	
	@Tool("Send text to the player and receive the player's reply. ")
	IFuture<String> getPlayerReply(String text);
}
