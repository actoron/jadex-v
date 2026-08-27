package jadex.llm.smarthome;

import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.core.Application;
import jadex.core.INoCopyStep;
import jadex.future.IFuture;
import jadex.micro.llmcall2.IRuleSystemService;
import jadex.micro.llmcall2.LlmHelper;
import jadex.micro.llmcall2.RuleSystem;
import jadex.requiredservice.IRequiredServiceFeature;

public class Main
{
	public static void main(String[] args)
	{		
		// Create the LLM that will control the smart home
		StreamingChatModel	model	= LlmHelper.createChatModel();	// Default Ollama model
//		StreamingChatModel	model	= LlmHelper.Provider.UNSLOTH.createChatModel(null,  null);
//		StreamingChatModel	model	= LlmHelper.Provider.OLLAMA_REMOTE.createChatModel("gemma4:26b-a4b-it-q4_K_M", false);
//		StreamingChatModel	model	= LlmHelper.Provider.GOOGLE_GEMINI.createChatModel("gemini-2.5-flash", true);
//		StreamingChatModel	model	= LlmHelper.Provider.GOOGLE_GEMINI.createChatModel("gemini-3-flash-preview", true);
//		StreamingChatModel	model	= LlmHelper.Provider.MISTRAL_AI.createChatModel("mistral-large-2512", false);
		
		// Create smart home components
		Application app = new Application("Smart Home");
		app.create(new RuleSystem(model), "Rule System").get();
		app.create(new Camera(), "Kamera 1").get();
		app.create(new Camera(), "Kamera 2").get();
		app.create(new Camera(), "Kamera 3").get();
		app.create(new MotionSensor(), "Bewegungsmelder A").get();
		app.create(new MotionSensor(), "Bewegungsmelder B").get();
		app.create(new Alarm(), "Alarm").get();
		
		// Create the GUI to visualize the smart home
		app.create(new MainGui()).get();
		
		// Get the rule system and send some prompts to it
		IRuleSystemService rulesystem = app
			.runAsync((INoCopyStep<IFuture<IRuleSystemService>>)
				comp -> comp.getFeature(IRequiredServiceFeature.class)
					.searchService(IRuleSystemService.class)).get();
		
		String	prompt;
		prompt	= 
			"Immer wenn ein Bewegungsmelder auslöst, analysiere das aktuelle Bild jeder Kamera "
			+ "und löse Alarm aus, wenn du eine verdächtige Situation bemerkst.";
		rulesystem.executePrompt(prompt).get();
		
		prompt	= 
			"Überprüfe alle 30 Sekunden die aktuellen Bilder von Kamera 2 und 3 "
			+ "und löse Alarm aus, wenn du eine verdächtige Situation bemerkst.";
		rulesystem.executePrompt(prompt).get();
	}
}
