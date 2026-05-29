package jadex.llm.impl;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.llm.ILlmFeature;
import jadex.llm.ILlmService;

public class GroqAgent extends LlmBaseAgent implements ILlmService
{
    protected ChatModel llm;

    public ChatModel createChatModel() 
    {
        llm = OpenAiChatModel.builder()
            .apiKey(apikey)
            //.baseUrl("https://api.groq.com/openai/v1")
            .baseUrl("https://api.x.ai/v1/chat/completions")
            .modelName(model) 
            .build();
        return llm;
    }

    public String resolveApiKey()
    {
        return System.getenv("GROQ_API_KEY");
    }

    @Override
    public IFuture<String> callLlm(String prompt) 
    {
        String answer = llm.chat(prompt);
        return new Future<>(answer);
    }

    public static void main(String[] args) 
	{
		IComponentHandle llm = IComponentManager.get().create(new GroqAgent()).get();
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
