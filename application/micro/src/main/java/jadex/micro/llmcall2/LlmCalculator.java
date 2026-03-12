package jadex.micro.llmcall2;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.impl.IDaemonComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

public class LlmCalculator
{
	@Service
	static interface ICalculator	extends IDaemonComponent
	{
		@Tool("Calculate the square root of a real number")
		IFuture<Double> sqrt(double a);
		
		@Tool(name="isqrt", value="Calculate the square root of a natural number")
		default IFuture<Integer> sqrt(int a)
		{
			return new Future<>(Integer.valueOf((int) Math.sqrt(a)));
		}
	}
	
	public static void main(String[] args) 
	{
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j", "debug");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j.model.ollama", "debug");

		IComponentManager.get().create((ICalculator) a -> new Future<>(Math.sqrt(a))).get();
		
		String	prompt	= "What is the square root of 144 and the square root of 15129?";
		
		StreamingChatModel llm =
			
//			GoogleAiGeminiStreamingChatModel.builder()
//			.thinkingConfig(GeminiThinkingConfig.builder()
//				.includeThoughts(true)
//				.build())
//			.apiKey(System.getenv("GOOGLE_API_KEY"))
////			.modelName("gemini-3-flash-preview")
//			.modelName("gemini-2.5-flash")
////			.modelName("gemini-2.5-flash-lite")
			
			OllamaStreamingChatModel.builder()
			.baseUrl("http://localhost:11434")
//			.think(false)
//			.modelName("gpt-oss:20b")
//			.modelName("mistral:7b-instruct")
//			.modelName("qwen3:4b")
			.modelName("qwen3.5:0.8b")
//			.modelName("qwen3:0.6b")
			
//			.modelName("llama3.2:1b-instruct-q4_K_M")
//			.modelName("functiongemma:latest")
//			.modelName("qwen2.5-coder:7b")
//			.modelName("qwen2.5:latest")
//			.modelName("lfm2.5-thinking:1.2b")
//			.modelName("ministral-3:3b")
//			.modelName("granite4:350m")
			
			.returnThinking(true)
			.logRequests(true)
//			.logResponses(true)
			.build();
		
		IComponentHandle	agent	= IComponentManager.get().create(new LlmResultAgent(llm, prompt)).get();
		int[] last = new int[1];
		agent.subscribeToResults().next(event ->
		{
			if(event.type()==Type.ADDED && event.name().equals("response"))
			{
				if(last[0]!=1)
				{
					System.out.println();
					last[0]=1;
				}
				System.out.print(event.value());
			}
			else if(event.type()==Type.ADDED && event.name().equals("thinking"))
			{
				if(last[0]!=2)
				{
					System.out.println();
					last[0]=2;
				}
				System.out.print("\033[3m"+event.value()+"\033[0m");
			}
			else if(event.type()==Type.ADDED && event.name().equals("toolcalls"))
			{
				if(last[0]!=3)
				{
					System.out.println();
					last[0]=3;
				}
				System.out.println("\033[3;1m"+event.value()+"\033[0m");
			}
		}).printOnEx();
		
		agent.waitForTermination().get();		
		IComponentManager.get().waitForLastComponentTerminated();
		System.exit(0);
	}
}