package jadex.llm.impl;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;
import jadex.llm.ILlmService;

public class GroqAgent implements ILlmService
{
    protected String apikey;
    protected String model;

    protected ChatModel llm;

    public GroqAgent()
    {
        this(System.getenv("GROQ_API_KEY"));
    }

    public GroqAgent(String apikey)
    {
        this(apikey, "llama-3.3-70b-versatile");
    }

    public GroqAgent(String apikey, String model)
    {
        this.apikey = apikey;
        this.model = model;
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
        System.out.println("agent started: "+agent.getId());
     
        llm = OpenAiChatModel.builder()
            .apiKey(apikey)
            .baseUrl("https://api.groq.com/openai/v1")
            .modelName(model) 
            .build();

        /*String answer = llm.chat(
            "Explain the difference between Jadex agent and llm agent in a single sentence."
        );
        System.out.println(answer);*/

        //agent.terminate();
    }

    public static void main(String[] args) 
	{
		IComponentManager.get().create(new GroqAgent("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")).get();
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
