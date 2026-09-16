package jadex.llm.twentyquestions;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.core.Application;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.llmcall2.ChatFragment;
import jadex.micro.llmcall2.LlmChatAgent;
import jadex.micro.llmcall2.LlmHelper;

public class TwentyQuestions implements ITwentyQuestionsService
{
	protected Map<String, String> gamestate = new LinkedHashMap<>();
	
	@Override
	public IFuture<String> getGameState(String key)
	{
		return new Future<>(gamestate.get(key));
	}
	
	@Override
	public IFuture<Void> setGameState(String key, String value)
	{
		gamestate.put(key, value);
		return IFuture.DONE;
	}
	
	@Override
	public IFuture<String> getPlayerReply(String text)
	{
		// Show a modal dialog to the player with the text and get the player's reply.
		Future<String>	ret	= new Future<>();
		SwingUtilities.invokeLater(() ->
		{
			String reply = JOptionPane.showInputDialog(null, text, "Twenty Questions", JOptionPane.QUESTION_MESSAGE);
			if (reply == null)
			{
				reply = "I want to stop the game";
			}
			ret.setResult(reply);
		});
		return ret;
	}
	
	public static void main(String[] args)
	{
		Application app = new Application("Twenty Questions");
		TwentyQuestions tq = new TwentyQuestions();
		app.create(tq).get();
		
		String	prompt = """
				# Role:
				You are a game **host** for the game "20 Questions":
				
				## Game Rules:
				- You choose a famous person as your secret.
				- The player has to guess the secret person by asking yes/no questions.
				- The player has 20 questions to guess the secret person.
				- The player can ask questions like "Is the person alive?", "Is the person an actor?", "Is the person a politician?", etc.
				- After each question, you answer the user with "Yes" or "No" and tell the player how many questions they have left.
				- If the player guesses the secret person correctly, you tell the player "Congratulations! You guessed the secret person!" and end the game.
				- If the player runs out of questions, you tell the player "Sorry, you ran out of questions. The secret person was [secret person]." and end the game.
				
				## Game Play:
				1. Choose a famous person as your secret and store the chosen person in the game state.
				2. Initialize the number of questions left to 20 in the game state.
				3. Start a loop where you ask the player for a yes/no question about the secret person:
				   In each iteration of the loop:
				    - Use the `get_player_reply` tool to send a welcome message or answer the last question
				      and get the (next) player's question.
					- Update the number of questions left in the game state.
					- If the player guesses the secret person correctly, congratulate them and end the game using the `get_player_reply` tool.
					- If the player runs out of questions, reveal the secret person and end the game using the `get_player_reply` tool.
				
				## Important Notes:
				The player can't see your text output, thus you **must use the tool** to talk to the player and get the player's reply in each turn.
				""";
		
		StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA_REMOTE, "qwen3.6:35b", true, false);
//		StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA_REMOTE, "gemma4:26b-a4b-it-q4_K_M", true, false);
//		StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.UNSLOTH, "unsloth/Qwen3.6-35B-A3B-MTP-GGUF:UD-Q2_K_XL", true, false);
//		StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.UNSLOTH, "unsloth/gemma-4-E4B-it-qat-GGUF", true, false);
//		StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.UNSLOTH, "unsloth/GLM-4.7-Flash-GGUF", true, false);
		ITerminableIntermediateFuture<ChatFragment>	chat	= app.runAsync(new LlmChatAgent(llm, prompt));
		LlmChatAgent.printResults(chat);
		
		app.waitForLastComponentTerminated();
	}
}
