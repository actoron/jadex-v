package jadex.micro.house_monitoring;

import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.IFuture;
import jadex.micro.llmcall2.LlmChatAgent;
import jadex.micro.llmcall2.LlmHelper;
import jadex.requiredservice.IRequiredServiceFeature;

public class Main
{
	public static void main(String[] args)
	{
		// Create smart home components
		IComponentManager.get().create(new RuleSystem(), "Rule System").get();
		IComponentManager.get().create(new Camera(), "Kamera 1").get();
		IComponentManager.get().create(new Camera(), "Kamera 2").get();
		IComponentManager.get().create(new Camera(), "Kamera 3").get();
		IComponentManager.get().create(new MotionSensor(), "Bewegungsmelder A").get();
		IComponentManager.get().create(new MotionSensor(), "Bewegungsmelder B").get();
		IComponentManager.get().create(new Alarm(), "Alarm").get();
		
		// Create the LLM agent that will control the smart home
		StreamingChatModel	model	= LlmHelper.createChatModel();	// Default Ollama model
//		StreamingChatModel	model	= LlmHelper.Provider.OLLAMA_REMOTE.createChatModel("gemma4:26b-a4b-it-q4_K_M", true);
//		StreamingChatModel	model	= LlmHelper.Provider.GOOGLE_GEMINI.createChatModel("gemini-2.5-flash", true);
//		StreamingChatModel	model	= LlmHelper.Provider.GOOGLE_GEMINI.createChatModel("gemini-3-flash-preview", true);
		IComponentManager.get().create(new LlmChatAgent(model)).get();
		
		// Create the GUI to visualize the smart home
		IComponentManager.get().create(new MainGui()).get();
		
		// Get the rule system and send some prompts to it
		IRuleSystemService rulesystem = IComponentManager.get()
			.runAsync((INoCopyStep<IFuture<IRuleSystemService>>)
				comp -> comp.getFeature(IRequiredServiceFeature.class)
					.searchService(IRuleSystemService.class)).get();
		
		String	prompt;
		prompt	= 
			"Immer wenn Bewegungsmelder A auslöst, analysiere das aktuelle Bild von Kamera 1 "
			+ "und löse Alarm aus, wenn du eine verdächtige Situation bemerkst.";
		rulesystem.executePrompt(prompt).get();
		
		prompt	= 
			"Überprüfe alle 30 Sekunden die aktuellen Bilder von Kamera 2 und 3 "
			+ "und löse Alarm aus, wenn du eine verdächtige Situation bemerkst.";
		rulesystem.executePrompt(prompt).get();
	}
}
