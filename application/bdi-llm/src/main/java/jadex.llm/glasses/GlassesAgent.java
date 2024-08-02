package jadex.llm.glasses;

import jadex.bdi.llm.impl.LlmFeature;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;



@Agent(type="bdip")
@Description("This agent uses ChatGPT to create the plan step.")
public class GlassesAgent
{
    /** The Glasses agent class. */
    @Agent
    protected IComponent agent;

    private final String chatUrl;
    private final String apiKey;
    private final String agentClassName;
    private final String featureClassName;

    private JSONObject dataset;

    /** Constructor */
    public GlassesAgent(String chatUrl, String apiKey, String agentClassName, String featureClassName, String dataSetPath)
    {
        this.chatUrl = chatUrl;
        this.apiKey = apiKey;
        this.agentClassName = agentClassName;
        this.featureClassName = featureClassName;


        System.out.println("A: " + chatUrl);
        System.out.println("A: " + apiKey);
        System.out.println("A: " + agentClassName);
        System.out.println("A: " + featureClassName);

        System.out.println("A: GlassesAgent class loaded");
        //Annotation

        //read Dateset jsonarray im constructor laden und befüllen
        String dataSetFileString = null;
        try {
            dataSetFileString = FileUtils.readFileToString(new File(dataSetPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            JSONParser parser = new JSONParser();
            this.dataset = (JSONObject) parser.parse(dataSetFileString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @OnStart
    public void body()
    {
        System.out.println("A: Agent " +agent.getId()+ " active");

        System.out.println(dataset);

        /** Initialize the LlmFeature */
        LlmFeature llmFeature = new LlmFeature(
                chatUrl,
                apiKey,
                agentClassName,
                featureClassName);

        llmFeature.connectToLLM("");
        System.out.println(llmFeature.generatedJavaCode);
        llmFeature.generateAndCompilePlanStep();
        llmFeature.doPlanStep(dataset);

        //System.out.println(newDataset);

        agent.terminate();
    }

    @OnEnd
    public void end()
    {
        System.out.println("A: Agent "+agent.getId()+ " terminated");
    }


    /**
     *  Start Glasses Agent.
     * @throws InterruptedException
     */
    public static void main(String[] args)
    {
        System.out.println("A: GlassesAgent started");

        IComponent.create(new GlassesAgent(
                "https://api.openai.com/v1/chat/completions",
                System.getenv("OPENAI_API_KEY"),
                "jadex.llm.glasses.GlassesAgent",
                "jadex.llm.glasses.Glasses",
                "C:/Users/resas/Documents/Coding/jadex-v/application/bdi-llm/src/main/java/jadex.llm/glasses/Dataset.json")
        );
        IComponent.waitForLastComponentTerminated();
    }
}
