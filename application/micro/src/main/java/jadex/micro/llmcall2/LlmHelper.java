package jadex.micro.llmcall2;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaModels;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

public class LlmHelper
{
	public static enum Provider
	{
		OLLAMA_LOCAL("Ollama (local)",
			(model, think) -> createOllamaChatModel("http://localhost:11434", model, think),
			() -> fetchOllamaModels("http://localhost:11434")),
		OLLAMA_REMOTE("Ollama (remote)", 
			(model, think) -> createOllamaChatModel(System.getenv("OLLAMA_BASE_URL"), model, think),
			() -> fetchOllamaModels(System.getenv("OLLAMA_BASE_URL"))),
		GOOGLE_GEMINI("Google Gemini",
			(model, think) -> createGoogleGeminiChatModel(model, think),
			() -> List.of(
				"gemini-3-flash-preview",
				"gemini-2.5-flash",
				"gemini-2.5-flash-lite")),
		OPEN_ROUTER("Open Router",
			(model, think) -> createOpenAiChatModel(model, think),
			() -> List.of(
				"openrouter/free",
				"stepfun/step-3.5-flash:free",
				"nvidia/nemotron-nano-12b-v2-vl:free",
				"mistralai/mistral-small-3.1-24b-instruct:free",
				"google/gemma-3-27b-it:free",
				"nvidia/nemotron-3-super-120b-a12b:free",
				"arcee-ai/trinity-large-preview:free",	// slow, but works well on blocksworld, breakfast and calculator
				"z-ai/glm-4.5-air:free",
				"qwen/qwen3-next-80b-a3b-instruct:free",
				"meta-llama/llama-3.3-70b-instruct:free"
		));
		
		private final String name;
		private final BiFunction<String, Boolean, StreamingChatModel> creator;
		private final Supplier<List<String>> modelfetcher;
		private Provider(String name, BiFunction<String, Boolean, StreamingChatModel> creator, Supplier<List<String>> modelfetcher)
		{
			this.name = name;
			this.creator = creator;
			this.modelfetcher = modelfetcher;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		public StreamingChatModel createChatModel(String model, boolean think)
		{
			if(model==null)
				model = DEFAULT_MODELS.get(this);
			if(model==null)
				model = getModels().get(0);
			return creator.apply(model, think);
		}
		
		public List<String> getModels()
		{
			return modelfetcher.get().stream().sorted().toList();
		}
	}
	
	public static final Map<Provider, String>	DEFAULT_MODELS = Collections.unmodifiableMap(
		Map.of(
			Provider.OLLAMA_LOCAL, 
				
				// Best models for blocksworld and breakfast
				"ministral-3:3b"	// fast and efficient, but no thinking
//				"devstral-small-2:24b"
//				"gpt-oss:20b"	// no image input
//				"qwen3.5:9b"
//				"qwen3:4b"	// no coffee :-(, fails on  blocksworld
//				"nemotron-cascade-2:30b"	// slow
				
				// Succeeds sometimes on breakfast
//				.modelName("nemotron-3-nano:30b")
//				.modelName("mistral-small3.2:24b")
//				.modelName("nemotron-3-nano:4b")
//				.modelName("granite4:3b")
//				.modelName("freakycoder123/phi4-fc:latest")
							
				// Fails on breakfast
//				.modelName("qwen3.5:0.8b")
//				.modelName("mistral:7b-instruct")
//				.modelName("qwen3:0.6b")
//				.modelName("60MPH/astral3-tools:12b")
//				.modelName("andrewmccall/gemma3-tools:latest")
//				.modelName("comethrusws/sage-reasoning:8b")
//				.modelName("cogito:8b")
//				.modelName("magistral:24b")
//				.modelName("nemotron-mini:4b")
//				.modelName("llama3-groq-tool-use:8b")
//				.modelName("mistral-nemo:12b-instruct-2407-q4_K_M")
//				.modelName("ministral-3:14b")
//				.modelName("llama3.1:8b-instruct-q4_K_M")
//				.modelName("functiongemma:latest")
//				.modelName("lfm2.5-thinking:1.2b")
//				.modelName("granite4:350m")
//				.modelName("phi4-mini:3.8b")
//				.modelName("command-r7b:latest")
//				.modelName("rnj-1:8b")
//				.modelName("patrickwarren2692/gemma-3-27b-it-abliterated-GGUF:latest")
//				.modelName("lfm2:24b")
			));
	
	public static StreamingChatModel createChatModel()
	{
		return createChatModel(null, null, true);
	}
	
	public static List<String>	fetchOllamaModels(String baseurl)
	{
		OllamaModels ollamaModels = OllamaModels.builder()
			.baseUrl(baseurl)
			.build();
		return ollamaModels.availableModels().content().stream().map(m -> m.getModel()).toList();
	}

	public static StreamingChatModel createChatModel(Provider provider, String model, boolean think)
	{
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j", "debug");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j.model.ollama", "debug");
		
		if(provider==null)
			provider = Provider.values()[0];
		
		return provider.createChatModel(model, think);
	}
	
	protected static StreamingChatModel	createOpenAiChatModel(String model, boolean think)
	{
		return OpenAiStreamingChatModel.builder()
			.baseUrl("https://openrouter.ai/api/v1")
			.apiKey(System.getenv("OPENAI_API_KEY"))
			.modelName(model)
			.returnThinking(think)
			.sendThinking(think)
//			.logRequests(true)
//			.logResponses(true)
			.build();
	}
	
	protected static StreamingChatModel createOllamaChatModel(String baseurl, String model, boolean think)
	{
		StreamingChatModel llm =
			OllamaStreamingChatModel.builder()
			.baseUrl(baseurl)
			.modelName(model)
			.returnThinking(think)
			.think(think)
//			.logRequests(true)
//			.logResponses(true)
			.build();
		return llm;
	}
	
	protected static StreamingChatModel	createGoogleGeminiChatModel(String model, boolean think)
	{
		return GoogleAiGeminiStreamingChatModel.builder()
			.thinkingConfig(GeminiThinkingConfig.builder()
				.includeThoughts(think)
				.build())
			.apiKey(System.getenv("GOOGLE_API_KEY"))
			.modelName(model)
			.returnThinking(think)
//			.logRequests(true)
//			.logResponses(true)
			.build();
	}
	
	/**
	 *  Creates a PNG image from the given component and returns it as a base64-encoded string.
	 */
	public static String createPngFromComponent(Component world)
	{
		if(world==null)
			throw new NullPointerException("Component must not be null.");

		Dimension size = world.getSize();
		if(size.width<=0 || size.height<=0)
		{
			Dimension preferred = world.getPreferredSize();
			if(preferred!=null)
				size = preferred;
		}
		if(size.width<=0 || size.height<=0)
			throw new IllegalArgumentException("Component has invalid size: "+size.width+"x"+size.height);

		BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		try
		{
			world.setSize(size);
			world.doLayout();
			world.printAll(g2);
		}
		finally
		{
			g2.dispose();
		}

		try(ByteArrayOutputStream baos = new ByteArrayOutputStream())
		{
			boolean written = ImageIO.write(image, "png", baos);
			if(!written)
				throw new IllegalStateException("No PNG writer available.");
			return Base64.getEncoder().encodeToString(baos.toByteArray());
		}
		catch(IOException e)
		{
			throw new RuntimeException("Failed to encode component image as PNG.", e);
		}
	}

	/**
	 *  Creates a PNG data URI from the given component.
	 */
	public static String createPngDataUriFromComponent(Component world)
	{
		return "data:image/png;base64," + createPngFromComponent(world);
	}
}