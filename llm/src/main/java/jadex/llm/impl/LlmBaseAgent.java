package jadex.llm.impl;

import dev.langchain4j.model.chat.ChatModel;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;

public abstract class LlmBaseAgent 
{
    protected String apikey;
    protected String model;
    protected String url;

    protected ChatModel llm;

    public LlmBaseAgent() 
    {
    }

    public LlmBaseAgent setApiKey(String apikey)
    {
        this.apikey = apikey;
        return this;
    }

    public String getApiKey()
    {
        if(apikey == null || apikey.isEmpty())
            apikey = resolveApiKey();
        return apikey;
    }

    public LlmBaseAgent setModel(String model)
    {
        this.model = model;
        return this;
    }

    public String getModel()
    {
        return model;
    }

    public LlmBaseAgent setUrl(String url)
    {
        this.url = url;
        return this;
    }

    public String getUrl()
    {
        return url;
    }

    public IFuture<String> callLlm(String prompt) 
    {
        String answer = llm.chat(prompt);
        return new Future<>(answer);
    }

    @OnStart
    protected void start(IComponent agent) 
    {
        //System.out.println("Agent started: " + agent.getId());

        /*if (apikey == null || apikey.isEmpty()) 
        {
            System.out.println("Warining, API key not set. Please set the API_KEY environment variable");
            //agent.terminate();
            //return;
        }*/

        llm = createChatModel();

        System.out.println("LlmAgent ready with model: " + model+" "+llm);
    }

    public abstract ChatModel createChatModel();

    public abstract String resolveApiKey();
}
