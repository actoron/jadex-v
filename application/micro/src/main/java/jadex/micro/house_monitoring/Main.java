package jadex.micro.house_monitoring;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.llmcall2.ChatFragment;
import jadex.micro.llmcall2.LlmChatAgent;
import jadex.micro.llmcall2.LlmHelper;

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
		
		IComponentManager.get().create(new MainGui()).get();
		
		// Create the LLM agent that will control the smart home
		IComponentHandle	llmagent	= IComponentManager.get()
			.create(new LlmChatAgent(LlmHelper.createChatModel())).get();
		LlmChatAgent llmpojo = (LlmChatAgent) llmagent.getPojoHandle(LlmChatAgent.class);
		String	prompt	= 
			"Immer wenn Bewegungsmelder A auslöst, analysiere das aktuelle Bild von Kamera 1 "
			+ "und löse Alarm aus, wenn du eine verdächtige Situation bemerkst.";
		System.out.println("User: "+prompt);
		ITerminableIntermediateFuture<ChatFragment>	fut	= llmpojo.chat(prompt);
		LlmChatAgent.printResults(fut);
		fut.get();
		
		prompt	= 
			"Immer wenn Bewegungsmelder B auslöst, analysiere die aktuellen Bilder von Kamera 2 und 3 "
			+ "und löse Alarm aus, wenn du eine verdächtige Situation bemerkst.";
		System.out.println("User: "+prompt);
		fut	= llmpojo.chat(prompt);
		LlmChatAgent.printResults(fut);
		fut.get();
	}
}
