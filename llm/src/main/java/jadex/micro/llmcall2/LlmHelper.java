package jadex.micro.llmcall2;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.anthropic.AnthropicModelCatalog;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
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
import dev.langchain4j.model.openai.OpenAiResponsesStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jadex.core.IComponentManager;
import jadex.future.ITerminableIntermediateFuture;

public class LlmHelper
{
	static
	{
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j", "debug");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j.model.ollama", "debug");		
	}
	
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
//			(model, think) -> createGoogleGenAiChatModel(model, think),
			() -> fetchGeminiModels(),
			(model) -> fetchGeminiContextSize(model)),
		MISTRAL_AI("Mistral AI",
			(model, think) -> createMistralChatModel(model, think),
			() -> fetchMistralModels(),
			(model) -> fetchMistralContextSize(model)),
		OPEN_ROUTER("Open Router",
			(model, think) -> createOpenAiChatModel("https://openrouter.ai/api/v1", System.getenv("OPENAI_API_KEY"), model, think),
			() -> fetchOpenAiModels("https://openrouter.ai/api/v1", System.getenv("OPENAI_API_KEY"), true),
			(model) -> fetchOpenAiContextSize("https://openrouter.ai/api/v1", System.getenv("OPENAI_API_KEY"), model)),
//		LOCAL_AI("Local AI",
//			(model, think) -> createLocalAiChatModel(model, think),
//			() -> fetchOpenAiModels("http://localhost:8080/v1", "", false),
//			(model) -> fetchOpenAiContextSize("http://localhost:8080/v1", "", model)),
//		LOCAL_AI("Local AI",
//			(model, think) -> createOllamaChatModel("http://localhost:8080", model, think),
//			() -> fetchOpenAiModels("http://localhost:8080/v1", "", false),
//			(model) -> fetchOllamaContextSize("http://localhost:8080", model)),
		LOCAL_AI("Local AI",
			(model, think) -> createOpenAiChatModel("http://localhost:8080/v1", "", model, think),
			() -> fetchOpenAiModels("http://localhost:8080/v1", "", false),
			(model) -> fetchOpenAiContextSize("http://localhost:8080/v1", "", model)),
		LM_STUDIO("LM Studio",
			(model, think) -> createOpenAiResponsesChatModel("http://localhost:1234/v1", "nix", model, think),
			() -> fetchOpenAiModels("http://localhost:1234/v1", "nix", false),
			(model) -> fetchOpenAiContextSize("http://localhost:1234/v1", "", model)),
		LLAMA_SERVER("Llama Server",
			(model, think) -> createOpenAiChatModel("http://localhost:8033/v1", "nix", model, think),
			() -> fetchOpenAiModels("http://localhost:8033/v1", "nix", false),
			(model) -> fetchOpenAiContextSize("http://localhost:8033/v1", "", model)),
		UNSLOTH("Unsloth",
			(model, think) -> createOpenAiResponsesChatModel("http://localhost:8000/v1", System.getenv("UNSLOTH_API_KEY"), model, think),
			() -> fetchOpenAiModels("http://localhost:8000/v1", System.getenv("UNSLOTH_API_KEY"), false),
			(model) -> fetchOpenAiContextSize("http://localhost:8000/v1", System.getenv("UNSLOTH_API_KEY"), model));
		
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
				
				// thinking
				"qwen3.5:9b"

				// no thinking
//				"qwen3.5:0.8b"
//				"gemma4:e2b"
//				"ministral-3:3b"
//				"granite4.1:3b"
//				"phi4-mini:3.8b"
//				"ministral-3:14b"
			));
	
	public static StreamingChatModel createChatModel()
	{
		return createChatModel(null, null, null);
//		return createChatModel(null, null, false);
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
		try
		{
			OllamaModels ollamaModels = OllamaModels.builder()
				.baseUrl(baseurl)
				.build();
			return ollamaModels.runningModels().content().stream()
				.filter(m -> m.getName().equals(model))
				.findFirst().map(m ->
			{
				System.out.println("Ollama model: "+m.getName());
				return m.getContextLength();
			})
				.orElse(-1);
		}
		catch(Exception e)
		{
			System.err.println("Failed to fetch Ollama context size: "+e);
			return -1;
		}
	}

	public static StreamingChatModel createChatModel(Provider provider, String model, Boolean think)
	{
		if(provider==null)
			provider = Provider.values()[0];
		
		return provider.createChatModel(model, think);
	}
	
	protected static StreamingChatModel	createOpenAiChatModel(String baseurl, String apikey, String model, Boolean think)
	{
		return OpenAiStreamingChatModel.builder()
			.baseUrl(baseurl)
			.apiKey(apikey)
			.modelName(model)
			// cf. https://developers.openai.com/api/docs/guides/reasoning
			.reasoningEffort(think!=null? (think ? "high" : "none") : null)
			// If there is thinking -> always use it.
			.returnThinking(true)
			.sendThinking(true)
//			.logRequests(true)
//			.logResponses(true)
			// For LM Studio, we need to force HTTP/1.1 :-(
			.httpClientBuilder(JdkHttpClient.builder()
				.httpClientBuilder(HttpClient.newBuilder()
					.version(HttpClient.Version.HTTP_1_1)))
			.build();
	}
		
	protected static StreamingChatModel	createOpenAiResponsesChatModel(String baseurl, String apikey, String model, Boolean think)
	{
		return OpenAiResponsesStreamingChatModel.builder()
			.baseUrl(baseurl)
			.apiKey(apikey)
			.modelName(model)
			.reasoningEffort(think!=null? (think ? "high" : "none") : null)
			.reasoningSummary("auto")
//			.logRequests(true)
//			.logResponses(true)
			// For LM Studio, we need to force HTTP/1.1 :-(
			// Also needed for Unsloth!?, otherwise we get strange errors
			.httpClientBuilder(JdkHttpClient.builder()
				.httpClientBuilder(HttpClient.newBuilder()
					.version(HttpClient.Version.HTTP_1_1)))
			.build();
	}
	
//	protected static StreamingChatModel	createLocalAiChatModel(String model, Boolean think)
//	{
//		return LocalAiStreamingChatModel.builder()
//			.baseUrl("http://localhost:8080/v1")
//			.modelName(model)
//			.logRequests(true)
//			.logResponses(true)
//			.build();
//	}
	
	protected static List<String>	fetchOpenAiModels(String baseurl, String apikey, boolean free)
	{
		ModelCatalog	cat	= OpenAiModelCatalog.builder()
			.baseUrl(baseurl)
			.apiKey(apikey)
			// For LM Studio, we need to force HTTP/1.1 :-(
			.httpClientBuilder(JdkHttpClient.builder()
				.httpClientBuilder(HttpClient.newBuilder()
					.version(HttpClient.Version.HTTP_1_1)))
			.build();
		return cat.listModels().stream()
//			.filter(m -> m.type()==null || m.type()==ModelType.CHAT)
			.map(m -> m.name())
			.filter(name -> !free || name.endsWith(":free"))
			.sorted().toList();
	}
	
	protected static int fetchOpenAiContextSize(String baseurl, String apikey, String model)
	{
		ModelCatalog	cat	= OpenAiModelCatalog.builder()
			.baseUrl(baseurl)
			.apiKey(apikey)
			.build();
		return cat.listModels().stream()
			.filter(m -> m.name().equals(model))
			.findFirst().map(m -> m.maxInputTokens()!=null ? m.maxInputTokens() : -1).orElse(-1);
	}
	
	protected static StreamingChatModel createAnthropicChatModel(String baseurl, String apikey, String model, Boolean think)
	{
		return AnthropicStreamingChatModel.builder()
			.baseUrl(baseurl)
			.apiKey(apikey)
			// For LM Studio, we need to force HTTP/1.1 :-(
			.httpClientBuilder(JdkHttpClient.builder()
				.httpClientBuilder(HttpClient.newBuilder()
					.version(HttpClient.Version.HTTP_1_1)))
			.modelName(model)
			.returnThinking(true)
			.sendThinking(true)
			.thinkingType(think ? "enabled": null)
			.thinkingDisplay("summarized")
			.logRequests(true)
			.logResponses(true)
			.build();
	}
	
	protected static List<String>	fetchAnthropicModels(String baseurl, String apikey)
	{
		ModelCatalog	cat	= AnthropicModelCatalog.builder()
			.baseUrl(baseurl)
			.apiKey(apikey)
			.build();
		return cat.listModels().stream()
//			.filter(m -> m.type()==ModelType.CHAT)
			.map(m -> m.name())
			.sorted()
			.toList();
	}
	
	protected static StreamingChatModel createOllamaChatModel(String baseurl, String model, Boolean think)
	{
		OllamaStreamingChatModelBuilder	llm	= OllamaStreamingChatModel.builder()
			.baseUrl(baseurl)
			.modelName(model)
//			.temperature(0.0)
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
	
//	protected static StreamingChatModel createGoogleGenAiChatModel(String model, Boolean think)
//	{
//		return GoogleGenAiStreamingChatModel.builder()
//			.apiKey(System.getenv("GOOGLE_API_KEY"))
//			.modelName(model)
//			.thinkingBudget(think!=null && !think ? 0 : null)
//			.build();
//	}
	
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
	
//	protected static StreamingChatModel createLocalAiChatModel(String model, Boolean think)
//	{
//		return LocalAiStreamingChatModel.builder()
//			.baseUrl("http://localhost:8080/v1")
//			.modelName(model)
//			.logRequests(true)
//			.build();
//	}
	
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
	 *  Creates an AWT image from the given component.
	 */
	public static RenderedImage	createImageFromComponent(Component world)
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

		BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
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
		
		return image;
	}
	
	/**
	 *  Create a langchain4j Image object from an AWT image.
	 */
	public static Image	createLangchainImage(RenderedImage image)
	{
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream())
		{
			boolean written = ImageIO.write(image, "jpg", baos);
			if(!written)
				throw new IllegalStateException("No JPEG writer available.");
			String	base64	= Base64.getEncoder().encodeToString(baos.toByteArray());
			
			return Image.builder()
				.base64Data(base64)
				.mimeType("image/jpeg")
				.build();
		}
		catch(IOException e)
		{
			throw new RuntimeException("Failed to encode component image as JPEG.", e);
		}

	}
}