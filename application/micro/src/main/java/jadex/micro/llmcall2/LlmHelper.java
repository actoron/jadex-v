package jadex.micro.llmcall2;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

public class LlmHelper
{
	public static StreamingChatModel createChatModel()
	{
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j", "debug");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j.model.ollama", "debug");
		
		StreamingChatModel llm =
			
//			GoogleAiGeminiStreamingChatModel.builder()
//			.thinkingConfig(GeminiThinkingConfig.builder()
//				.includeThoughts(true)
//				.build())
//			.apiKey(System.getenv("GOOGLE_API_KEY"))
////			.modelName("gemini-3-flash-preview")
////			.modelName("gemini-2.5-flash")
//			.modelName("gemini-2.5-flash-lite")
			
			OllamaStreamingChatModel.builder()
			.baseUrl("http://localhost:11434")
//			.think(false)
//			.modelName("devstral-small-2:24b")
//			.modelName("gpt-oss:20b")
//			.modelName("qwen3.5:9b")
//			.modelName("qwen3:4b")	// no coffee :-(
//			.modelName("qwen3.5:2b-q4_K_M")
			
			// Succeeds sometimes on breakfast
//			.modelName("nemotron-3-nano:30b")
//			.modelName("mistral-small3.2:24b")
//			.modelName("nemotron-3-nano:4b")
//			.modelName("granite4:3b")
//			.modelName("freakycoder123/phi4-fc:latest")
			
			// Fails on breakfast, but succeeds on blocksworld
			.modelName("ministral-3:3b")
			
			// Fails on breakfast
//			.modelName("qwen3.5:0.8b")
//			.modelName("mistral:7b-instruct")
//			.modelName("qwen3:0.6b")
//			.modelName("60MPH/astral3-tools:12b")
//			.modelName("andrewmccall/gemma3-tools:latest")
//			.modelName("comethrusws/sage-reasoning:8b")
//			.modelName("cogito:8b")
//			.modelName("magistral:24b")
//			.modelName("nemotron-mini:4b")
//			.modelName("llama3-groq-tool-use:8b")
//			.modelName("mistral-nemo:12b-instruct-2407-q4_K_M")
//			.modelName("ministral-3:14b")
//			.modelName("llama3.1:8b-instruct-q4_K_M")
//			.modelName("functiongemma:latest")
//			.modelName("lfm2.5-thinking:1.2b")
//			.modelName("granite4:350m")
//			.modelName("phi4-mini:3.8b")
//			.modelName("command-r7b:latest")
//			.modelName("rnj-1:8b")
//			.modelName("patrickwarren2692/gemma-3-27b-it-abliterated-GGUF:latest")
//			.modelName("lfm2:24b")

			// Fails due to wrong response format (missing "tool_calls" field)
//			.modelName("hermes3:3b")
//			.modelName("llama3.2:1b-instruct-q4_K_M")
//			.modelName("qwen2.5-coder:7b")
//			.modelName("qwen2.5:latest")
//			.modelName("cogito:3b")
			
			.returnThinking(true)
//			.logRequests(true)
//			.logResponses(true)
			.build();
		return llm;
	}
}
