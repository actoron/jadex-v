package jadex.llm.twentyquestions;

import dev.langchain4j.agent.tool.Tool;
import jadex.core.impl.IDaemonComponent;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ITwentyQuestionsService	extends IDaemonComponent
{	
	@Tool("Send text to the user and receive the user's reply. ")
	IFuture<String> getPlayerReply(String text);
	
	@Tool("Get the value of the game state for the given key.")
	IFuture<String> getGameState(String key);
	
	@Tool("Set a key/value of the game state.")
	IFuture<Void> setGameState(String key, String value);
}
