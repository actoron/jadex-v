package jadex.llm.glasses;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.impl.inmemory.IPlanBody;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.runtime.Val;
import jadex.core.*;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.FileUtils;



@Agent(type="bdip")
@Description("This agent uses ChatGPT to create the plan step.")
public class GlassesAgent extends ResultProvider
{
    /** The Glasses agent class. */
    @Agent
    protected IComponent agent;

    private final String chatUrl;
    private final String apiKey;
//    private JSONObject dataset;
    private final String dataSetPath;
    private final String beliefType;

    private long time;
    private final Map<String, Object> timeMeasure;

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
            System.out.println("A: Goal finished");
            System.out.println("Data: " + convDataSetString.get());
            //timetracking from connect to llm to goal finished
            time = System.currentTimeMillis() - time;
            System.out.println("Time: " + time);
            agent.terminate();
        }

        @GoalTargetCondition(parameters="convDataSetString")
        public boolean checkTarget()
        {

            System.out.println("--->Test Goal");
            LlmFeature llmFeature = new LlmFeature(
                    chatUrl,
                    apiKey,
                    beliefType,
                    "application/bdi-llm/src/main/java/jadex.llm/glasses/settings/GoalSettings.json");

            llmFeature.connectToLLM("");

            IPlanBody plan = llmFeature.generateAndCompileCode();
            JSONParser parser = new JSONParser();
            JSONObject convDataSet = null;
            try {
                convDataSet = (JSONObject) parser.parse(convDataSetString.get());
                ArrayList<Object> inputList = new ArrayList<Object>();
                inputList.add(convDataSet);

                ArrayList<Object> outputList = plan.runCode(inputList);
                Boolean checkStatus  = (Boolean) outputList.get(0);

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

    /** Constructor */
    public GlassesAgent(String chatUrl, String apiKey, String dataSetPath)
    {
        this.chatUrl = chatUrl;
        this.apiKey = apiKey;
        this.beliefType = "java.util.ArrayList";
        this.dataSetPath = dataSetPath;

        System.out.println("A: GlassesAgent class loaded");
        timeMeasure = Map.of();
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
    }

    @Plan(trigger=@Trigger(goals=AgentGoal.class))
    protected void generatePlan1(AgentGoal goal) {
        /** Initialize the LlmFeature */
        System.out.println("--->Test Plan 1");

        LlmFeature llmFeature = new LlmFeature(
                chatUrl,
                apiKey,
                beliefType,
                "application/bdi-llm/src/main/java/jadex.llm/glasses/settings/Plan1Settings.json");

        System.out.println(datasetString);
        System.out.println("time start");
        time = System.currentTimeMillis();
        llmFeature.connectToLLM("");

        IPlanBody plan = llmFeature.generateAndCompileCode();
        JSONParser parser = new JSONParser();
        try {
            JSONObject dataset = (JSONObject) parser.parse(goal.getConvDataSetString());
            ArrayList<Object> inputList = new ArrayList<Object>();
            inputList.add(dataset);

            ArrayList<Object> outputList = plan.runCode(inputList);

            JSONObject convDataSet = (JSONObject) outputList.get(0);
            System.out.println("A: Plan 1 finished");
            goal.setConvDataSetString(convDataSet.toString());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Plan(trigger=@Trigger(goals=AgentGoal.class))
    protected void generatePlan2(AgentGoal goal)
    {
        /** Initialize the LlmFeature */
        System.out.println("--->Test Plan 2");
        LlmFeature llmFeature = new LlmFeature(
                chatUrl,
                apiKey,
                beliefType,
                "application/bdi-llm/src/main/java/jadex.llm/glasses/settings/Plan2Settings.json");

        time = System.currentTimeMillis();
        llmFeature.connectToLLM("");

        IPlanBody plan = llmFeature.generateAndCompileCode();
        JSONParser parser = new JSONParser();
        try {
            JSONObject dataset = (JSONObject) parser.parse(goal.getConvDataSetString());
            ArrayList<Object> inputList = new ArrayList<Object>();
            inputList.add(dataset);

            ArrayList<Object> outputList = plan.runCode(inputList);

            JSONObject convDataSet = (JSONObject) outputList.get(0);
            goal.setConvDataSetString(convDataSet.toString());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
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
        int max = 5;
        for(int i = 0; i < max; i++)
        {
            System.out.println("A: GlassesAgent started iteration " + i);
            GlassesAgent currentAgent = new GlassesAgent(
                    "https://api.openai.com/v1/chat/completions",
                    System.getenv("OPENAI_API_KEY"),
                    "application/bdi-llm/src/main/java/jadex.llm/glasses/Dataset.json");

            System.out.println("CREATE AGENT");
            IExternalAccess exta = IComponentManager.get().create(currentAgent).get();
            System.out.println("CREATED AGENT");
            Semaphore s = new Semaphore(0);
            exta.waitForTermination().then(o -> {
                System.out.println("TERMINATED AGENT");
                s.release();
            });
            System.out.println("try acquire");
            try {
                s.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("acquired");

//            exta.waitForTermination().get();
            //System.out.println("FINISHED AGENT");

        }
    }
}
