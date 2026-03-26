package jadex.llm.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;
import jadex.llm.ILlmFeature;
import jadex.llm.ILlmService;

public class OllamaAgent extends LlmBaseAgent implements ILlmService
{
    public static final String PREFERRED_MODEL = "llama3.1";//:8b";

    public static final String OLLAMA_API_URL = "http://localhost:11434";

    public static final String OLLAMA_IMAGE = "ollama/ollama:latest";
    
    public static final String OLLAMA_BASE_URL = System.getenv("OLLAMA_BASE_URL");
    
    public static OllamaContainer ollama;

    protected ChatModel llm;

    public OllamaAgent()
    {
    }

    public ChatModel createChatModel() 
    {
        this.llm = OllamaChatModel.builder()
            .baseUrl(url)
            .modelName(model)
            .logRequests(true)
            .build();
        return llm;
    }

    public String resolveApiKey()
    {
        return System.getenv("OLLAMA_API_KEY");
    }

    @OnStart
    protected void start(IComponent agent) 
    {
        if(model==null || model.isEmpty())
            this.model = PREFERRED_MODEL;
        if(url==null || url.isEmpty())
            this.url = resolveOllamaBaseUrl();
     
        System.out.println("agent started: "+agent.getId()+" url: "+url+" preferred model: "+model);
        
        //String url = resolveOllamaBaseUrl();

        List<String> models = listModels(url);

        model = findPreferredModel(models, model);

        if (model == null) 
        {
            System.out.println("No preferred model found, selecting random chat model...");
            model = findAnyChatModel(url, models, true);
        }

        if(model==null)
        {
            System.out.println("No suitable chat model found.");
            agent.terminate();
        }
        else
        {
            System.out.println("Using model: " + model);
            super.start(agent);
        }
    }

    @Override
    public IFuture<String> callLlm(String prompt) 
    {
        if(llm!=null)
            return new Future<>(llm.chat(prompt));
        else
            return new Future<>(new RuntimeException("No LLM available"));
    }

    public static String resolveOllamaBaseUrl() 
    {
        // 1. explicitly set?
        if (OLLAMA_BASE_URL != null && !OLLAMA_BASE_URL.isEmpty()) 
            return OLLAMA_BASE_URL;

        // 2. local instance?
        if (isOllamaAlive(OLLAMA_API_URL)) 
        {
            System.out.println("Using local Ollama at " + OLLAMA_API_URL);
            return OLLAMA_API_URL;
        }

        // 3. Fallback: container
        System.out.println("No local Ollama found, starting container...");
        startContainer();
        return ollama.getEndpoint();
    }

    public static String findAnyChatModel(String baseUrl, List<String> models, boolean fast) 
    {
        List<String> cmodels = new ArrayList<>();

        for (String m : models) 
        {
            if (isChatModel(baseUrl, m)) 
            {
                cmodels.add(m);
                if(fast)
                    break;
            }
        }

        if (cmodels.isEmpty())
            return null;

        // random
        int idx = (int)(Math.random() * cmodels.size());
        return cmodels.get(idx);
    }

    public static String findPreferredModel(List<String> models, String preferred) 
    {
        // 1. exact
        if (models.contains(preferred))
            return preferred;

        // 2. normalized
        String norm = normalizeModelName(preferred);
        for (String m : models)
            if (normalizeModelName(m).equals(norm))
                return m;

        // 3. fuzzy 
        for (String m : models)
            if (m.toLowerCase().contains(norm))
                return m;

        return null;
    }

    public static String normalizeModelName(String name) 
    {
        // remove paths
        if (name.contains("/"))
            name = name.substring(name.lastIndexOf("/") + 1);

        // remove parameters
        if (name.contains(":"))
            name = name.substring(0, name.indexOf(":"));

        return name.toLowerCase();
    }

    public List<String> listModels(String url)
    {
        HttpClient client = HttpClient.newHttpClient();

        // Unterstützte Model-Endpoints
        String[] endpoints = {
            "/api/tags",     // Ollama
            "/v1/models"     // OpenAI kompatibel
        };

        for (String endpoint : endpoints)
        {
            try
            {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url + endpoint))
                    .timeout(Duration.ofSeconds(3))
                    .GET();

                if (getApiKey() != null && !getApiKey().isBlank())
                    builder.header("Authorization", "Bearer " + getApiKey());

                HttpRequest request = builder.build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200)
                    continue;

                JsonObject root = Json.parse(response.body()).asObject();

                // 🔹 OpenAI Format: { "data": [ { "id": ... } ] }
                if (root.get("data") != null)
                {
                    JsonArray arr = root.get("data").asArray();
                    List<String> models = new ArrayList<>();

                    for (Object o : arr)
                    {
                        JsonObject obj = (JsonObject) o;
                        models.add(obj.get("id").asString());
                    }

                    return models;
                }

                // 🔹 Ollama Format: { "models": [ { "name": ... } ] }
                if (root.get("models") != null)
                {
                    JsonArray arr = root.get("models").asArray();
                    List<String> models = new ArrayList<>();

                    for (Object o : arr)
                    {
                        JsonObject obj = (JsonObject) o;
                        models.add(obj.get("name").asString());
                    }

                    return models;
                }
            }
            catch (Exception e)
            {
                // next endpoint
            }
        }

        return List.of();
    }

    public static boolean isChatModel(String baseUrl, String modelName) 
    {
        boolean ret = false;

        System.out.println("Checking if model " + modelName + " is a chat model...");
        try 
        {
            HttpClient client = HttpClient.newHttpClient();

            String body = """
            {
            "model": "%s",
            "stream": false,
            "messages": [
                { "role": "user", "content": "ping" }
            ]
            }
            """.formatted(modelName);

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200)
            {   
                JsonObject root = Json.parse(resp.body()).asObject();
                if (root.get("error") == null)
                    ret = true;
            }
        } 
        catch (Exception e) 
        {
            System.out.println("Model " + modelName + " exception: " + e.getMessage());
        }

        if(ret)
            System.out.println("Model " + modelName + " is a chat model");
        else
            System.out.println("Model " + modelName + " is NOT a chat model");

        return ret;
    }    

    public static boolean isOllamaAlive(String baseUrl) 
    {
        try 
        {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } 
        catch (Exception e) 
        {
            return false;
        }
    }

    public static void startContainer() 
    {
        if (ollama != null) 
            return;

        String image = localOllamaImage(PREFERRED_MODEL);
        ollama = new OllamaContainer(resolve(OLLAMA_IMAGE, image));
        ollama.start();
        try 
        {
            ollama.execInContainer("ollama", "pull", PREFERRED_MODEL);
        } 
        catch (Exception e) 
        {
            throw new RuntimeException("Failed to pull model", e);
        }
        ollama.commitToImage(image);
    }

    public static String localOllamaImage(String name) 
    {
        return String.format("tc-%s-%s", OLLAMA_IMAGE, name);
    }

    public static DockerImageName resolve(String base, String image) 
    {
        DockerImageName name = DockerImageName.parse(base);
        DockerClient client = DockerClientFactory.instance().client();
        List<Image> images = client.listImagesCmd().withReferenceFilter(image).exec();
        if (images.isEmpty()) 
            return name;
        return DockerImageName.parse(image).asCompatibleSubstituteFor(base);
    }

	/*public static void main(String[] args) 
	{
		IComponentManager.get().create(new OllamaAgent()).get();
		IComponentManager.get().waitForLastComponentTerminated();
	}*/

    public static void main(String[] args) 
	{
		IComponentHandle llm = IComponentManager.get().create(new OllamaAgent()).get();
        IComponentManager.get().getFeature(ILlmFeature.class)
            .handleToolCall("What is the capital of France?")
            .then(answer -> 
            {
                System.out.println("Final answer: " + answer);
                llm.terminate();
            }).catchEx(e -> e.printStackTrace());
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
