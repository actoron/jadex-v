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
import java.util.function.Function;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import dev.langchain4j.model.catalog.ModelCatalog;
import dev.langchain4j.model.catalog.ModelType;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.model.googleai.GeminiThinkingConfig.GeminiThinkingLevel;
import dev.langchain4j.model.googleai.GoogleAiGeminiModelCatalog;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.mistralai.MistralAiModelCatalog;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaModels;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel.OllamaStreamingChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiModelCatalog;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;
import jadex.core.IComponentManager;
import jadex.future.ITerminableIntermediateFuture;

public class LlmHelper
{
	public static enum Provider
	{
		OLLAMA_LOCAL("Ollama (local)",
			(model, think) -> createOllamaChatModel("http://localhost:11434", model, think),
			() -> fetchOllamaModels("http://localhost:11434"),
			(model) -> fetchOllamaContextSize("http://localhost:11434", model)),
		OLLAMA_REMOTE("Ollama (remote)", 
			(model, think) -> createOllamaChatModel(System.getenv("OLLAMA_BASE_URL"), model, think),
			() -> fetchOllamaModels(System.getenv("OLLAMA_BASE_URL")),
			(model) -> fetchOllamaContextSize(System.getenv("OLLAMA_BASE_URL"), model)),
		GOOGLE_GEMINI("Google Gemini",
			(model, think) -> createGoogleGeminiChatModel(model, think),
			() -> fetchGeminiModels(),
			(model) -> fetchGeminiContextSize(model)),
		MISTRAL_AI("Mistral AI",
				(model, think) -> createMistralChatModel(model, think),
				() -> fetchMistralModels(),
				(model) -> fetchMistralContextSize(model)),
		OPEN_ROUTER("Open Router",
			(model, think) -> createOpenAiChatModel("https://openrouter.ai/api/v1", model, think),
			() -> fetchOpenAiModels("https://openrouter.ai/api/v1", true),
			(model) -> fetchOpenAiContextSize("https://openrouter.ai/api/v1", model)),
		LOCAL_AI("Local AI",
			(model, think) -> createOpenAiChatModel("http://localhost:8080/v1", model, think),
			() -> fetchOpenAiModels("http://localhost:8080/v1", false),
			(model) -> fetchOpenAiContextSize("http://localhost:8080/v1", model));
		
		private final String name;
		private final BiFunction<String, Boolean, StreamingChatModel> creator;
		private final Supplier<List<String>> modelfetcher;
		private final Function<String, Integer> contextfetcher;
		private Provider(String name, BiFunction<String, Boolean, StreamingChatModel> creator,
			Supplier<List<String>> modelfetcher, Function<String, Integer> contextfetcher)
		{
			this.name = name;
			this.creator = creator;
			this.modelfetcher = modelfetcher;
			this.contextfetcher = contextfetcher;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		public StreamingChatModel createChatModel(String model, Boolean think)
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
		
		public int getContextSize(String model)
		{
			return contextfetcher.apply(model);
		}
	}
	
	public static final Map<Provider, String>	DEFAULT_MODELS = Collections.unmodifiableMap(
		Map.of(
			Provider.OLLAMA_LOCAL, 
				
				// Best models for blocksworld and breakfast
//				"ministral-3:3b"	// fast and efficient, but no thinking
//				"devstral-small-2:24b"
//				"gpt-oss:20b"	// no image input
//				"qwen3.5:9b"
//				"qwen3:4b"	// no coffee :-(, fails on  blocksworld
//				"nemotron-cascade-2:30b"	// slow
				"gemma4:31b"
				
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
//				.modelName("patrickwarren2692/gemma-3-27b-it-abliterated-GGUF:latest")
//				.modelName("lfm2:24b")
			));
	
	public static StreamingChatModel createChatModel()
	{
		return createChatModel(null, null, null);
	}
	
	protected static int	fetchMistralContextSize(String model)
	{
		MistralAiModelCatalog	catalog	= MistralAiModelCatalog.builder()
			.apiKey(System.getenv("MISTRAL_API_KEY"))
			.build();
		return catalog.listModels().stream()
			.filter(m -> m.name().equals(model))
			.findFirst()
			.map(m -> m.maxInputTokens()!=null ? m.maxInputTokens() : -1)
			.orElse(-1);
	}

	protected static List<String>	fetchMistralModels()
	{
		MistralAiModelCatalog	catalog	= MistralAiModelCatalog.builder()
			.apiKey(System.getenv("MISTRAL_API_KEY"))
			.build();
		return catalog.listModels().stream().filter(m -> m.type()==null || m.type()==ModelType.CHAT).map(m -> m.name()).sorted().toList();
	}

	protected static StreamingChatModel createMistralChatModel(String model, Boolean think)
	{
		return MistralAiStreamingChatModel.builder()
			.apiKey(System.getenv("MISTRAL_API_KEY"))
			.modelName(model)
			// No way to enable/disable thinking in Mistral?
			.returnThinking(true)
			.sendThinking(true)
			.build();
	}

	protected static List<String>	fetchOllamaModels(String baseurl)
	{
		OllamaModels ollamaModels = OllamaModels.builder()
			.baseUrl(baseurl)
			.build();
		return ollamaModels.availableModels().content().stream()
//			.filter(m ->
//		{
////			System.out.println("Ollama model: "+m.getName());
//			OllamaModelCard	card	= ollamaModels.modelCard(m).content();
////			System.out.println("  capabilities: "+card.getCapabilities());
//			return card.getCapabilities().contains("completion") && card.getCapabilities().contains("tools");
//		})
			.map(m -> m.getName()).sorted().toList();
	}
	
	protected static int fetchOllamaContextSize(String baseurl, String model)
	{
		OllamaModels ollamaModels = OllamaModels.builder()
			.baseUrl(baseurl)
			.build();
		return ollamaModels.runningModels().content().stream()
			.filter(m -> m.getName().equals(model))
			.findFirst().map(m ->
		{
			System.out.println("Ollama model: "+m.getName());
			return -1;
		})
			.orElse(-1);
	}

	public static StreamingChatModel createChatModel(Provider provider, String model, Boolean think)
	{
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j", "debug");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j.model.ollama", "debug");
		
		if(provider==null)
			provider = Provider.values()[0];
		
		return provider.createChatModel(model, think);
	}
	
	protected static StreamingChatModel	createOpenAiChatModel(String baseurl, String model, Boolean think)
	{
		OpenAiStreamingChatModelBuilder	oscmb	= OpenAiStreamingChatModel.builder()
			.baseUrl(baseurl)
			.apiKey(System.getenv("OPENAI_API_KEY"))
			.modelName(model)
			// cf. https://developers.openai.com/api/docs/guides/reasoning
			.reasoningEffort(think!=null? (think ? "high" : "none") : null)
			// If there is thinking -> always use it.
			.returnThinking(true)
			.sendThinking(true)
			.logRequests(true)
//			.logResponses(true)
			;
				
		return oscmb.build();
	}
	
	protected static List<String>	fetchOpenAiModels(String baseurl, boolean free)
	{
		ModelCatalog	cat	= OpenAiModelCatalog.builder()
			.baseUrl(baseurl)
			.apiKey(System.getenv("OPENAI_API_KEY"))
			.build();
		return cat.listModels().stream()
			.filter(m -> m.type()==null || m.type()==ModelType.CHAT)
			.map(m -> m.name())
			.filter(name -> !free || name.endsWith(":free"))
			.sorted().toList();
	}
	
	protected static int fetchOpenAiContextSize(String baseurl, String model)
	{
		ModelCatalog	cat	= OpenAiModelCatalog.builder()
			.baseUrl(baseurl)
			.apiKey(System.getenv("OPENAI_API_KEY"))
			.build();
		return cat.listModels().stream()
			.filter(m -> m.name().equals(model))
			.findFirst().map(m -> m.maxInputTokens()!=null ? m.maxInputTokens() : -1).orElse(-1);
	}
	
	protected static StreamingChatModel createOllamaChatModel(String baseurl, String model, Boolean think)
	{
		OllamaStreamingChatModelBuilder	llm	= OllamaStreamingChatModel.builder()
			.baseUrl(baseurl)
			.modelName(model)
//			.logRequests(true)
//			.logResponses(true)
			// If there is thinking -> always use it.
			.returnThinking(true);
			
		if(think!=null)
			llm.think(think);
		return llm.build();
	}
	
	protected static List<String>	fetchGeminiModels()
	{
		ModelCatalog cat = GoogleAiGeminiModelCatalog.builder()
			.apiKey(System.getenv("GOOGLE_API_KEY"))
			.build();
		return cat.listModels().stream().filter(m -> m.type()==ModelType.CHAT).map(m -> m.name()).sorted().toList();
	}
	
	protected static int fetchGeminiContextSize(String model)
	{
		ModelCatalog cat = GoogleAiGeminiModelCatalog.builder()
			.apiKey(System.getenv("GOOGLE_API_KEY"))
			.build();
		return cat.listModels().stream().filter(m -> m.name().equals(model)).findFirst()
			.map(m -> m.maxInputTokens()!=null ? m.maxInputTokens() : -1).orElse(-1);
	}
	
	protected static StreamingChatModel	createGoogleGeminiChatModel(String model, Boolean think)
	{
		GeminiThinkingConfig.Builder	gtcb	= GeminiThinkingConfig.builder()
			// If there is thinking -> always use it.
			.includeThoughts(true);
		
		// Not supported for 2.x models
		if(think!=null && model.startsWith("gemini-3"))
		{
			// TODO: support other levels?
			gtcb.thinkingLevel(think ? GeminiThinkingLevel.HIGH
				: GeminiThinkingLevel.MINIMAL);
		}
		else if(think!=null && !think && model.startsWith("gemini-2"))
		{
			gtcb.thinkingBudget(0);
		}
		
		return GoogleAiGeminiStreamingChatModel.builder()
			.apiKey(System.getenv("GOOGLE_API_KEY"))
			.modelName(model)
			.thinkingConfig(gtcb.build())
			// If there is thinking -> always use it.
			.returnThinking(true)
			.sendThinking(true)
//			.logRequests(true)
//			.logResponses(true)
			.build();
	}
	
	/**
	 *  Is the model thinking?
	 */
	public static boolean isThinking(StreamingChatModel llm)
	{
		ITerminableIntermediateFuture<ChatFragment>	fut	= IComponentManager.get()
			.runAsync(new LlmChatAgent(llm, "Are you thinking?"));
//		LlmChatAgent.printResults(fut);
		String response = LlmChatAgent.getResponse(fut);
		String thinking = LlmChatAgent.getThinking(fut);
		System.out.println("Thinking: "+thinking);
		System.out.println("Response: "+response);
		return thinking!=null && !thinking.isEmpty();
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