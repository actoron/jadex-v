package jadex.llm.glasses;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.impl.InMemoryClass;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.runtime.Val;
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

//    private JSONObject dataset;
    private final String dataSetPath;

    /** Constructor */
    public GlassesAgent(String chatUrl, String apiKey, String agentClassName, String featureClassName, String dataSetPath)
    {
        this.chatUrl = chatUrl;
        this.apiKey = apiKey;
        this.agentClassName = agentClassName;
        this.featureClassName = featureClassName;
        this.dataSetPath = dataSetPath;


        System.out.println("A: " + chatUrl);
        System.out.println("A: " + apiKey);
        System.out.println("A: " + agentClassName);
        System.out.println("A: " + featureClassName);

        System.out.println("A: GlassesAgent class loaded");
    }

    @Belief
    private Val<JSONObject> dataset;

    @Goal
    public class AgentGoal
    {
        @GoalParameter
        protected Val<JSONObject> convDataSet;

        @GoalCreationCondition(beliefs="dataset")
        public AgentGoal(JSONObject convDataSet)
        {
            this.convDataSet = new Val<>(convDataSet);
            System.out.println("A: Goal created");
        }

        @GoalTargetCondition(parameters="convDataSet")
        public boolean checkTarget()
        {
            System.out.println("A: Goal check");
            //todo return true if the goal is achieved
            return true;
        }

        public void setConvDataSet(JSONObject val)
        {
            convDataSet.set(val);
        }
    }

    @OnStart
    public void body()
    {
        System.out.println("A: Agent " +agent.getId()+ " active");

        //read Dateset jsonarray im constructor laden und befüllen
        String dataSetFileString = null;
        try {
            dataSetFileString = FileUtils.readFileToString(new File(dataSetPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            JSONParser parser = new JSONParser();
            dataset.set((JSONObject) parser.parse(dataSetFileString));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        System.out.println(dataset);

        agent.terminate();
    }

    @Plan(trigger=@Trigger(goals=AgentGoal.class))
    protected void generatePlan1(AgentGoal goal)
    {
        /** Initialize the LlmFeature */
        LlmFeature llmFeature = new LlmFeature(
                chatUrl,
                apiKey,
                agentClassName,
                featureClassName);

//        System.out.println(llmFeature.readClassStructure(agentClassName, featureClassName));
        llmFeature.connectToLLM("");
        System.out.println(llmFeature.generatedJavaCode);

        llmFeature.generateAndCompilePlan();
        InMemoryClass plan = llmFeature.generateAndCompilePlan();
        JSONObject sortDataSet = plan.doPlan(dataset.get());
        goal.setConvDataSet(sortDataSet);
//        System.out.println(sortDataSet);
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
                "/home/schuther/IdeaProjects/jadex-v/application/bdi-llm/src/main/java/jadex.llm/glasses/Dataset.json")
        );
        IComponent.waitForLastComponentTerminated();
    }
}
