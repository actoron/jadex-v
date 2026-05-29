package jadex.llm.impl;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.llm.ILlmFeature;
import jadex.llm.ILlmService;

public class OpenRouterAgent extends LlmBaseAgent implements ILlmService 
{
    protected String apikey;
    protected String model;

    protected ChatModel llm;

    public OpenRouterAgent() 
    {
    }

    public ChatModel createChatModel() 
    {
        llm = OpenAiChatModel.builder()
            .apiKey(getApiKey())
            .baseUrl("https://openrouter.ai/api/v1")
            .modelName(getModel())
            .build();
        return llm;
    }

    public String resolveApiKey()
    {
        return System.getenv("OPENROUTER_API_KEY");
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