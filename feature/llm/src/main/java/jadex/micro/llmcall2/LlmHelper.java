package jadex.micro.llmcall2;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

<<<<<<< HEAD
import org.apache.commons.lang3.function.TriFunction;
import dev.langchain4j.agent.tool.Tool;

=======
import dev.langchain4j.agent.tool.Tool;
>>>>>>> 0bdf2a148e3ff265d421d9915f5d53ccbaca87e0
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.anthropic.AnthropicModelCatalog;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.catalog.ModelCatalog;
import dev.langchain4j.model.catalog.ModelType;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.model.googleai.GeminiThinkingConfig.GeminiThinkingLevel;
import dev.langchain4j.model.googleai.GoogleAiGeminiModelCatalog;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.mistralai.MistralAiModelCatalog;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaModels;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiModelCatalog;
import dev.langchain4j.model.openai.OpenAiResponsesStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jadex.common.SUtil;
import jadex.common.TimeoutException;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.providedservice.IService;
import jadex.providedservice.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

public class LlmHelper
{
	static
	{
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j", "debug");
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j.model.ollama", "debug");		
		System.setProperty("org.slf4j.simpleLogger.log.dev.langchain4j.model.openai", "debug");
	}
	
	public static enum Provider
	{
<<<<<<< HEAD
		OLLAMA_LOCAL("Ollama (local)",
			(model, think, json) -> createOllamaChatModel("http://localhost:11434", model, think),
			() -> fetchOllamaModels("http://localhost:11434"),
			(model) -> fetchOllamaContextSize("http://localhost:11434", model)),
		OLLAMA_REMOTE("Ollama (remote)", 
			(model, think, json) -> createOllamaChatModel(System.getenv("OLLAMA_BASE_URL"), model, think),
			() -> fetchOllamaModels(System.getenv("OLLAMA_BASE_URL")),
			(model) -> fetchOllamaContextSize(System.getenv("OLLAMA_BASE_URL"), model)),
		OPENAI_HCI("Ollama (remote)", 
			(model, think, json) -> createOpenAiChatModel(System.getenv("OPENAI_BASE_URL"), System.getenv("OPENAI_API_KEY"),model, think, json),
			() -> fetchOllamaModels(System.getenv("OPENAI_BASE_URL")),
			(model) -> fetchOllamaContextSize(System.getenv("OPENAI_BASE_URL"), model)),
=======
		OLLAMA("Ollama", 
			(model, think) -> createOllamaChatModel(System.getenv("OLLAMA_BASE_URL"), model, think),
			() -> fetchOllamaModels(System.getenv("OLLAMA_BASE_URL")),
			(model) -> fetchOllamaContextSize(System.getenv("OLLAMA_BASE_URL"), model)),
//		OLLAMA_LOCAL("Ollama (local)",
//		(model, think) -> createOllamaChatModel("http://localhost:11434", model, think),
//		() -> fetchOllamaModels("http://localhost:11434"),
//		(model) -> fetchOllamaContextSize("http://localhost:11434", model)),
>>>>>>> 0bdf2a148e3ff265d421d9915f5d53ccbaca87e0
		GOOGLE_GEMINI("Google Gemini",
			(model, think, json) -> createGoogleGeminiChatModel(model, think),
//			(model, think) -> createGoogleGenAiChatModel(model, think),
			() -> fetchGeminiModels(),
			(model) -> fetchGeminiContextSize(model)),
		MISTRAL_AI("Mistral AI",
			(model, think, json) -> createMistralChatModel(model, think),
			() -> fetchMistralModels(),
			(model) -> fetchMistralContextSize(model)),
		OPEN_ROUTER("Open Router",
			(model, think, json) -> createOpenAiChatModel("https://openrouter.ai/api/v1", System.getenv("OPENAI_API_KEY"), model, think, json),
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
<<<<<<< HEAD
		LOCAL_AI("Local AI",
			(model, think, json) -> createOpenAiChatModel("http://localhost:8080/v1", "", model, think, json),
			() -> fetchOpenAiModels("http://localhost:8080/v1", "", false),
			(model) -> fetchOpenAiContextSize("http://localhost:8080/v1", "", model)),
		LM_STUDIO("LM Studio",
			(model, think, json) -> createOpenAiResponsesChatModel("http://localhost:1234/v1", "nix", model, think),
			() -> fetchOpenAiModels("http://localhost:1234/v1", "nix", false),
			(model) -> fetchOpenAiContextSize("http://localhost:1234/v1", "", model)),
		LLAMA_SERVER("Llama Server",
			(model, think, json) -> createOpenAiChatModel("http://localhost:8033/v1", "nix", model, think, json),
			() -> fetchOpenAiModels("http://localhost:8033/v1", "nix", false),
			(model) -> fetchOpenAiContextSize("http://localhost:8033/v1", "", model)),
		UNSLOTH("Unsloth",
			(model, think, json) -> createOpenAiResponsesChatModel("http://localhost:8000/v1", System.getenv("UNSLOTH_API_KEY"), model, think),
			() -> fetchOpenAiModels("http://localhost:8000/v1", System.getenv("UNSLOTH_API_KEY"), false),
			(model) -> fetchOpenAiContextSize("http://localhost:8000/v1", System.getenv("UNSLOTH_API_KEY"), model));
=======
//		LOCAL_AI("Local AI",
//			(model, think) -> createOpenAiChatModel("http://localhost:8080/v1", "", model, think),
//			() -> fetchOpenAiModels("http://localhost:8080/v1", "", false),
//			(model) -> fetchOpenAiContextSize("http://localhost:8080/v1", "", model)),
//		LM_STUDIO("LM Studio",
//			(model, think) -> createOpenAiResponsesChatModel("http://localhost:1234/v1", "nix", model, think),
//			() -> fetchOpenAiModels("http://localhost:1234/v1", "nix", false),
//			(model) -> fetchOpenAiContextSize("http://localhost:1234/v1", "", model)),
//		LLAMA_SERVER("Llama Server",
//			(model, think) -> createOpenAiChatModel("http://localhost:8033/v1", "nix", model, think),
//			() -> fetchOpenAiModels("http://localhost:8033/v1", "nix", false),
//			(model) -> fetchOpenAiContextSize("http://localhost:8033/v1", "", model)),
		UNSLOTH("Unsloth",
			(model, think) -> createOpenAiResponsesChatModel("http://localhost:8888/v1", System.getenv("UNSLOTH_API_KEY"), model, think),
			() -> fetchOpenAiModels("http://localhost:8888/v1", System.getenv("UNSLOTH_API_KEY"), false),
			(model) -> fetchOpenAiContextSize("http://localhost:8888/v1", System.getenv("UNSLOTH_API_KEY"), model));
>>>>>>> 0bdf2a148e3ff265d421d9915f5d53ccbaca87e0
		
		private final String name;
		private final TriFunction<String, Boolean, Boolean, StreamingChatModel> creator;
		private final Supplier<List<String>> modelfetcher;
		private final Function<String, Integer> contextfetcher;

		private Provider(String name, TriFunction<String, Boolean, Boolean, StreamingChatModel> creator,
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
		
		public StreamingChatModel createChatModel(String model, Boolean think, Boolean json)
		{
			if(model==null)
				model = DEFAULT_MODELS.get(this);
			if(model==null)
				model = getModels().get(0);
			return creator.apply(model, think, json);
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
			Provider.OLLAMA, "gemma4:31b",
//			Provider.UNSLOTH, "unsloth/gemma-4-12B-it-qat-GGUF"
			Provider.UNSLOTH, "unsloth/Ministral-3-3B-Instruct-2512-GGUF:UD-Q4_K_XL"
		));
	
	public static StreamingChatModel createChatModel()
	{
		return createChatModel(null, null, null, null);
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

	public static StreamingChatModel createChatModel(Provider provider, String model, Boolean think, Boolean json)
	{
		if(provider==null)
			provider = Provider.values()[0];
		
		return provider.createChatModel(model, think, json);
	}

	public static StreamingChatModel createOpenAiChatModel(String baseurl, String apikey, String model, Boolean think, boolean json)
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
<<<<<<< HEAD
			//.logRequests(true)
			//.logResponses(true)
			.responseFormat(json? ResponseFormat.JSON: ResponseFormat.TEXT)
			// For LM Studio, we need to force HTTP/1.1 :-(
			.httpClientBuilder(JdkHttpClient.builder()
				.httpClientBuilder(HttpClient.newBuilder()
					.version(HttpClient.Version.HTTP_1_1)))
=======
//			.logRequests(true)
//			.logResponses(true)
//			// For LM Studio, we need to force HTTP/1.1 :-(
//			.httpClientBuilder(JdkHttpClient.builder()
//				.httpClientBuilder(HttpClient.newBuilder()
//					.version(HttpClient.Version.HTTP_1_1)))
>>>>>>> 0bdf2a148e3ff265d421d9915f5d53ccbaca87e0
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
//			.httpClientBuilder(JdkHttpClient.builder()
//				.httpClientBuilder(HttpClient.newBuilder()
//					.version(HttpClient.Version.HTTP_1_1)))
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
//			.logRequests(true)
//			.logResponses(true)
//			// For LM Studio, we need to force HTTP/1.1 :-(
//			.httpClientBuilder(JdkHttpClient.builder()
//				.httpClientBuilder(HttpClient.newBuilder()
//					.version(HttpClient.Version.HTTP_1_1)))
			.build();
		return cat.listModels().stream()
			.filter(m -> m.type()==null || m.type()==ModelType.CHAT)
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
		Map<String, String> headers = new HashMap<>();

		String apiKey = System.getenv("OLLAMA_API_KEY");
		if(apiKey != null && !apiKey.isBlank())
			headers.put("X-API-Key", apiKey);

		return OllamaStreamingChatModel.builder()
			.baseUrl(baseurl)
			.customHeaders(headers)
			.modelName(model)
			.think(think)
			.returnThinking(true)
			.build();
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
			.runAsync(new LlmChatAgent(llm, "Perform some chain-of-thoughts reasoning and then answer with the final result."));
		LlmChatAgent.printResults(fut);
		
		try
		{
			// Long timeout, because some models take a long time to load.
			fut.get(60000);
		}
		catch(TimeoutException e)
		{
			fut.terminate();
			System.err.println("Timeout while checking if model is thinking: "+e);
		}
		return fut.getIntermediateResults().stream().filter(f -> f.type()==ChatFragment.Type.THINKING).count() > 0;
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

	public static String cleanJsonResponse(String text)
	{
		text = text.trim();

		if(text.startsWith("```"))
		{
			int newline = text.indexOf('\n');
			int closing = text.lastIndexOf("```");

			if(newline >= 0 && closing > newline)
				text = text.substring(newline + 1, closing).trim();
		}

		return text;
	}

	public static Map<String, ToolRef> findTools(IComponent agent, Map<String, ToolRef> tools)
	{
		Set<Object>	found_services = new LinkedHashSet<>();
		Map<String, ToolRef> ret = new HashMap<>();
		if(tools!=null)
			ret.putAll(tools);
		
		Collection<?> services = agent.getFeature(IRequiredServiceFeature.class)
			.getLocalServices(new ServiceQuery<>((Class<?>)null).setServiceAnnotations(Tool.class));

		for(Object service : services)
		{
//			System.out.println("Found service: " + service);
			found_services.add(service);
			
			// Skip services that are already added as tools, to avoid creating new names when duplicate names exist.
			if(ret.values().stream().anyMatch(tool -> tool!=null && tool.service().equals(service)))
			{
				continue;
			}
			
			Class<?> type = ((IService)service).getServiceId().getServiceType().getType0();
			Collection<String>	tags = ((IService)service).getServiceId().getTags();
			for(Method m: type.getMethods())
			{
				if(m.isAnnotationPresent(Tool.class))
				{
					ToolSpecification	tool	= ToolSpecifications.toolSpecificationFrom(m);
					
					// Convert name to snake case if not explicitly set in annotation, as this is more common for tools (e.g. python function calls)
					String	name	= tool.name();
					if(m.getAnnotation(Tool.class).name().isEmpty())
					{
						name = SUtil.toSnakeCase(name);
					}
					
					// Append tags to description.
					String	description	= tool.description()==null ? "" : tool.description();
					description += (tags==null || tags.isEmpty() ? ""
						: (description.isBlank() ? "" : "\n")+	"Tags: "+String.join(", ", tags));
					
					// Create new tool specification with adjusted name and description.
					tool = ToolSpecification.builder()
						.name(name)
						.description(description)
						.parameters(tool.parameters())
						.metadata(tool.metadata())
						.build();

					// If tool with name already exists -> append suffix to existing, too.
					ToolRef existing = ret.get(name);
					if(existing!=null)
					{
						// Set to null to rename subsequent tools too.
						ret.put(name, null);
						
						ToolRef	new_tool = appendSuffix(existing, ret);
						ret.put(new_tool.spec().name(), new_tool);
					}
					
					// Append suffix to new tool if name already exists.
					ToolRef	tool_ref	= new ToolRef(tool, service, m);
					if(ret.containsKey(name))
					{
						tool_ref	= appendSuffix(tool_ref, ret);
					}
						
					ret.put(tool_ref.spec().name(), tool_ref);
				}
			}
		}
		
		for(ToolRef tool: new ArrayList<>(ret.values()))
		{
			// Remove tools that don't exist anymore
			if(tool!=null && !found_services.contains(tool.service()))
			{
				ret.remove(tool.spec().name());
			}
		}
		
		return ret;//.values().stream().filter(tool -> tool!=null).map(tool -> tool.spec()).toList();
	}

	public static ToolRef findTool(IComponent agent, String name)
	{
		Map<String, ToolRef> tools = findTools(agent, null);
		return tools.get(name);
	}

	/**
	 * 	Append unique suffix to tool name to avoid duplicate tool names.
	 */
	public static ToolRef appendSuffix(ToolRef existing, Map<String, ToolRef> tools)
	{
		String	name = existing.spec().name();
		while(tools.containsKey(name))
		{
			name = existing.spec().name()+"_"+UUID.randomUUID().toString().substring(0, 3);
		}
		return new ToolRef(ToolSpecification.builder()
			.name(name)
			.description(existing.spec().description())
			.parameters(existing.spec().parameters())
			.metadata(existing.spec().metadata())
			.build(), existing.service(), existing.method());
	}

	public static IFuture<Object> callTool(IComponent agent, String toolname, Map<String, Object> parameters)
	{
		try
		{
			ToolRef tool = findTool(agent, toolname);
			if(tool == null)
				throw new RuntimeException("Tool not found: " + toolname);

			IService service = (IService)tool.service();
			Method m = tool.method();

			//Map<String, Object> args = Json.fromJson(call.toolExecutionRequest().arguments(), Map.class);

			List<Object> param_values = new ArrayList<>();

			for(int i = 0; i < m.getParameters().length; i++)
			{
				if(!parameters.containsKey(m.getParameters()[i].getName()))
				{
					throw new RuntimeException(
						"Missing argument: "
						+ m.getParameters()[i].getName());
				}

				Object value = parameters.get(m.getParameters()[i].getName());

				// Convert value to parameter type if needed.
				if(value != null
					&& !m.getParameters()[i].getType().isAssignableFrom(value.getClass()))
				{
					value = Json.fromJson(Json.toJson(value), m.getParameters()[i].getType());
				}

				param_values.add(value);
			}

			Object result = m.invoke(service, param_values.toArray());

			if(result instanceof IFuture)
			{
				@SuppressWarnings("unchecked")
				IFuture<Object> resfut = (IFuture<Object>)result;
				return resfut;
			}
			else
			{
				return new Future<>(result);
			}
		}
		catch(Exception e)
		{
			return new Future<>(e);
		}
	}

	public static void addToolResult(List<ChatMessage> messages, CompleteToolCall call, Object result, boolean isvoid, StreamingChatModel llm)
	{
		ToolExecutionResultMessage msg;

		if(result instanceof RenderedImage)
		{
			msg = ToolExecutionResultMessage.builder()
				.id(call.toolExecutionRequest().id())
				.toolName(call.toolExecutionRequest().name())
				.contents(ImageContent.from(
					createLangchainImage((RenderedImage)result)))
				.build();
		}
		else
		{
			msg = ToolExecutionResultMessage.from(
				call.toolExecutionRequest(),
				isvoid && result==null
					? "done"
					: result instanceof String
						? (String)result
						: Json.toJson(result));
		}

		// Hack: currently important fields aren't passed back by
		// Ollama mapping, so add them manually here.
		if(llm instanceof OllamaStreamingChatModel)
		{
			if(msg.hasSingleText())
			{
				String text =
					"id=" + msg.id()
					+ ", tool_name=" + msg.toolName()
					+ ", result=" + msg.text();

				messages.add(
					ToolExecutionResultMessage.from(
						call.toolExecutionRequest(), text));
			}
			else
			{
				// Ollama only supports text content in tool results.
				List<Content> contents = new ArrayList<>();

				contents.add(TextContent.from(
					"id=" + msg.id()
					+ ", tool_name=" + msg.toolName()
					+ ", result=see attached contents"));

				contents.addAll(msg.contents());

				messages.add(UserMessage.from(contents));
			}
		}
		else if((llm instanceof MistralAiStreamingChatModel || llm.getClass().getName().contains("GoogleGenAiStreamingChatModel"))
			&& !msg.hasSingleText())
		{
			// Mistral/Google GenAI only support text content in tool results.

			int i = messages.size();

			while(i > 0 && messages.get(i - 1) instanceof UserMessage)
			{
				i--;
			}

			messages.add(
				i,
				ToolExecutionResultMessage.from(
					call.toolExecutionRequest(),
					"result=see user message"));

			List<Content> contents = new ArrayList<>();

			contents.add(TextContent.from(
				"id=" + msg.id()
				+ ", tool_name=" + msg.toolName()
				+ ", result=see attached contents"));

			contents.addAll(msg.contents());

			messages.add(UserMessage.from(contents));
		}
		else
		{
			messages.add(msg);
		}
	}

	public static String sanitizeJson(String text)
	{
		StringBuilder ret = new StringBuilder(text.length());

		boolean inString = false;
		boolean escaped = false;

		for(int i=0; i<text.length(); i++)
		{
			char c = text.charAt(i);

			if(escaped)
			{
				ret.append(c);
				escaped = false;
				continue;
			}

			if(c == '\\')
			{
				ret.append(c);
				escaped = true;
				continue;
			}

			if(c == '"')
			{
				ret.append(c);
				inString = !inString;
				continue;
			}

			if(inString)
			{
				switch(c)
				{
					case '\n':
						ret.append("\\n");
						break;

					case '\r':
						ret.append("\\r");
						break;

					case '\t':
						ret.append("\\t");
						break;

					default:
						ret.append(c);
				}
			}
			else
			{
				ret.append(c);
			}
		}

		return ret.toString();
	}

}