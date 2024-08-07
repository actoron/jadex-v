package jadex.llm.glasses;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.ILlmFeature;
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
    private Val<String> datasetString;

    @Goal
    public class AgentGoal
    {
        @GoalParameter
        protected Val<String> convDataSetString; //Val<String> convDataSet;

        @GoalCreationCondition(beliefs="datasetString")
        public AgentGoal(String convDataSetString)
        {
            this.convDataSetString = new Val<>(convDataSetString);
            System.out.println("A: Goal created");
        }

        @GoalFinished
        public void goalFinished() {
            System.out.println("goal finished");
        }

        @GoalTargetCondition(parameters="convDataSetString")
        public boolean checkTarget()
        {
            System.out.println("--->Test Goal");
            ILlmFeature llmFeature = new LlmFeature(
                    chatUrl,
                    apiKey,
                    agentClassName,
                    featureClassName,
                    "bdi/llm/src/main/java/jadex/bdi/llm/impl/GoalSettings.json");

            llmFeature.connectToLLM("");
            llmFeature.generateAndCompilePlan();
            InMemoryClass plan = llmFeature.generateAndCompilePlan();
            JSONParser parser = new JSONParser();
            JSONObject convDataSet = null;
            try {
                convDataSet = (JSONObject) parser.parse(convDataSetString.get());
                Boolean checkStatus  = (Boolean) plan.runCode(convDataSet);
                System.out.println("A: Goal check: " + checkStatus);
                return checkStatus;
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        public void setConvDataSetString(String val)
        {
            convDataSetString.set(val);
        }
        public String getConvDataSetString()
        {
            return convDataSetString.get();
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
            JSONObject dataset = (JSONObject) parser.parse(dataSetFileString);
            datasetString.set(dataset.toString());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        System.out.println(datasetString);

        agent.terminate();
    }

    @Plan(trigger=@Trigger(goals=AgentGoal.class))
    protected void generatePlan1(AgentGoal goal)
    {
        /** Initialize the LlmFeature */
        System.out.println("--->Test Plan");
        LlmFeature llmFeature = new LlmFeature(
                chatUrl,
                apiKey,
                agentClassName,
                featureClassName,
                "bdi/llm/src/main/java/jadex/bdi/llm/impl/PlanSettings.json");

//        System.out.println(llmFeature.readClassStructure(agentClassName, featureClassName));
        llmFeature.connectToLLM("");
        System.out.println(llmFeature.generatedJavaCode);

        llmFeature.generateAndCompilePlan();
        InMemoryClass plan = llmFeature.generateAndCompilePlan();
        JSONParser parser = new JSONParser();
        try {
            JSONObject dataset = (JSONObject) parser.parse(goal.getConvDataSetString());
            JSONObject convDataSet = (JSONObject) plan.runCode(dataset);
            goal.setConvDataSetString(convDataSet.toString());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

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
