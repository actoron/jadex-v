package jadex.llm.impl;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;
import jadex.llm.ILlmFeature;
import jadex.llm.ILlmService;

public class OpenRouterAgent implements ILlmService 
{
    protected String apikey;
    protected String model;

    protected ChatModel llm;

    public OpenRouterAgent() 
    {
        this(System.getenv("OPENROUTER_API_KEY"));
    }

    public OpenRouterAgent(String apikey) 
    {
        this(apikey, "openai/gpt-3.5-turbo"); 
    }

    public OpenRouterAgent(String apikey, String model) 
    {
        this.apikey = apikey;
        this.model = model;
    }

    public OpenRouterAgent setApiKey(String apikey)
    {
        this.apikey = apikey;
        return this;
    }

    public OpenRouterAgent setModel(String model)
    {
        this.model = model;
        return this;
    }
    
    @Override
    public IFuture<String> callLlm(String prompt) 
    {
        String answer = llm.chat(prompt);
        return new Future<>(answer);
    }

    @OnStart
    protected void start(IComponent agent) 
    {
        System.out.println("Agent started: " + agent.getId());

        if (apikey == null || apikey.isEmpty()) 
        {
            System.err.println("OPENROUTER API key not set. Please set the OPENROUTER_API_KEY environment variable.");
            agent.terminate();
            return;
        }

        llm = OpenAiChatModel.builder()
            .apiKey(apikey)
            .baseUrl("https://openrouter.ai/api/v1")
            .modelName(model)
            .build();

        System.out.println("OpenRouterAgent ready with model: " + model+" "+llm);
    }

    public static void main(String[] args) 
    {
        IComponentHandle agentHandle = IComponentManager.get().create(new OpenRouterAgent()).get();

        IComponentManager.get().getFeature(ILlmFeature.class)
            .handleToolCall("What is the capital of France?")
            .then(answer -> {
                System.out.println("Final answer: " + answer);
                agentHandle.terminate();
            })
            .catchEx(e -> e.printStackTrace());

        IComponentManager.get().waitForLastComponentTerminated();
    }
}