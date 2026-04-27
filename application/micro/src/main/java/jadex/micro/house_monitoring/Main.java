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
		IComponentManager.get().create(new IRuleSystemService(){}).get();
		IComponentManager.get().create(new Camera(), "Kamera 1").get();
		IComponentManager.get().create(new MotionSensor(), "Bewegungsmelder A").get();
		IComponentManager.get().create(new Alarm(), "Alarm").get();
		
		IComponentManager.get().create(new MainGui()).get();
		
		// Create the LLM agent that will control the smart home
		IComponentHandle	llmagent	= IComponentManager.get()
			.create(new LlmChatAgent(LlmHelper.createChatModel())).get();
		LlmChatAgent llmpojo = (LlmChatAgent) llmagent.getPojoHandle(LlmChatAgent.class);
		ITerminableIntermediateFuture<ChatFragment>	fut	= llmpojo.chat(
			"Wenn Bewegungsmelder A auslöst, analysiere das Bild von Kamera 1"
			+ "und löse Alarm aus, wenn du eine verdächtige Situation bemerkst.");
		LlmChatAgent.printResults(fut);
		
		fut.get();
	}
}
