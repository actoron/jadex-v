package jadex.llm.glasses;

import com.google.common.collect.Ordering;
import jadex.bdi.annotation.*;
import jadex.bdi.llm.impl.inmemory.IPlanBody;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.runtime.Val;
import jadex.core.*;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.io.FileUtils;


@Agent(type = "bdip")
@Description("This agent uses ChatGPT to create the plan step.")
public class GlassesAgent
{
    private final String chatUrl;
    private final String apiKey;
    private final String dataSetPath;
    private final String beliefType;
    /**
     * The OpticanData agent class.
     */
    @Agent
    protected IComponent agent;
    // Using thread-safe ConcurrentHashMap
    private HashMap<String, String> agentResults = new HashMap<>();

    @Belief
    private Val<String> datasetString;

    @Goal
    public class AgentGoal
    {
        @GoalParameter
        protected Val<String> convDataSetString; // Val<String> convDataSet;
        protected IPlanBody goalPlan = null;

        @GoalCreationCondition(beliefs = "datasetString")
        public AgentGoal(String convDataSetString)
        {
            //Constructor Agent
            this.convDataSetString = new Val<>(convDataSetString);
            System.out.println("A: Goal created");
        }

        @GoalFinished
        public void goalFinished()
        {
            System.out.println("A: Goal finished");
            agent.terminate();
        }

        @GoalTargetCondition(parameters = "convDataSetString")
        public boolean checkTarget()
        {
            System.out.println("--->Test Goal");
            //############################################################################################################
            //static GoalCheck
            //############################################################################################################
            boolean goalCheck = false;

            JSONParser parser = new JSONParser();
            try
            {
                JSONObject resultDataSet = (JSONObject) parser.parse(convDataSetString.get());
                // check if resultDataSet is sorted by shape
                JSONArray glasses = (JSONArray) resultDataSet.get("OpticanData");
                // get key shape from all glasses
                // check if shapes are sorted
                ArrayList<String> shapes = new ArrayList<String>();
                for (Object glass : glasses)
                {
                    JSONObject glassObject = (JSONObject) glass;
                    String glassShape = (String) glassObject.get("shape");
                    shapes.add(glassShape);
                }

                goalCheck = Ordering.natural().isOrdered(shapes);

            } catch (ParseException e)
            {
                throw new RuntimeException(e);
            }

            System.out.println("A: static Goal check: " + goalCheck);

            agentResults.put("staticGoalCheck", String.valueOf(goalCheck));

            //############################################################################################################
            //LLM GoalCheck
            //############################################################################################################
            LlmFeature llmFeature = new LlmFeature(
                    chatUrl,
                    apiKey,
                    beliefType,
                    "application/bdi-llm/src/main/java/jadex.llm/glasses/settings/GoalSettings_ollama.json");

            if (this.goalPlan == null)
            {
                int attempt = 0;
                while (attempt < 3)
                {
                    llmFeature.connectToLLM("");
                    System.out.println("~~Attempt: " + attempt);
                    System.out.println("~~GoalCode: " + llmFeature.generatedJavaCode);
                    try
                    {
                        this.goalPlan = llmFeature.generateAndCompileCode(true);

                        if (this.goalPlan != null)
                        {
                            System.out.println("~~Code generated");
                            break; // Exit the loop if a valid plan is generated
                        }
                    } catch (Exception e)
                    {
                        System.out.println("~~Exception: " + e);
                        System.out.println("RETRY");
                        attempt++;
                    }
                }
                agentResults.put("generatedGoalCode1", llmFeature.generatedJavaCode);
                agentResults.put("goalSettings", llmFeature.getLlmSettings());
                agentResults.put("genGoalAttempts1", String.valueOf(attempt));
            }

            try
            {
                JSONObject convDataSet = (JSONObject) parser.parse(convDataSetString.get());
                ArrayList<Object> inputList = new ArrayList<Object>();
                inputList.add(convDataSet);

                ArrayList<Object> outputList = this.goalPlan.runCode(inputList);
                Boolean checkStatus = (Boolean) outputList.get(0);

                System.out.println("A: LLM Goal check: " + checkStatus);

                agentResults.put("chattyGoalCheck", String.valueOf(checkStatus));
            } catch (ParseException e)
            {
                throw new RuntimeException(e);
            }

            return goalCheck;


        }

        public String getConvDataSetString()
        {
            return convDataSetString.get();
        }

        public void setConvDataSetString(String val)
        {
            convDataSetString.set(val);
        }
    }

    /**
     * Constructor
     */
    public GlassesAgent(String chatUrl, String apiKey, String dataSetPath)
    {
        this.chatUrl = chatUrl;
        this.apiKey = apiKey;
        this.beliefType = "java.util.ArrayList";
        this.dataSetPath = dataSetPath;

        System.out.println("A: GlassesAgent class loaded");
    }

    @OnStart
    public void body()
    {
        System.out.println("A: Agent " + agent.getId() + " active");
        agentResults.put("agentId", agent.getId().toString());

        //read Dateset jsonarray im constructor laden und befüllen
        String dataSetFileString = null;
        try
        {
            dataSetFileString = FileUtils.readFileToString(new File(dataSetPath), StandardCharsets.UTF_8);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        try
        {
            JSONParser parser = new JSONParser();
            JSONObject dataset = (JSONObject) parser.parse(dataSetFileString);
            datasetString.set(dataset.toString());

        } catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getAgentResults()
    {
        return this.agentResults;
    }

    @Plan(trigger = @Trigger(goals = AgentGoal.class))
    protected void generatePlan1(AgentGoal goal)
    {
        /** Initialize the LlmFeature */
        System.out.println("--->Test Plan 1");

        LlmFeature llmFeature = new LlmFeature(
                chatUrl,
                apiKey,
                beliefType,
                "application/bdi-llm/src/main/java/jadex.llm/glasses/settings/Plan1Settings_ollama.json");

        IPlanBody plan = null;
        int attempt = 0;
        while (attempt < 3)
        {
            long genStart = System.currentTimeMillis();
            llmFeature.connectToLLM("");
            System.out.println("~~Attempt: " + attempt);
            System.out.println("~~GoalCode: " + llmFeature.generatedJavaCode);
            long genTime = System.currentTimeMillis() - genStart;
            agentResults.put("genTime1", String.valueOf(genTime));
            try
            {
                plan = llmFeature.generateAndCompileCode(true);

                if (plan != null)
                {
                    System.out.println("~~Code generated");
                    break; // Exit the loop if a valid plan is generated
                }
            } catch (Exception e)
            {
                System.out.println("~~Exception: " + e);
                System.out.println("RETRY");
                attempt++;
            }
        }
        agentResults.put("generatedPlanCode1", llmFeature.generatedJavaCode);
        agentResults.put("planSettings", llmFeature.getLlmSettings());
        agentResults.put("genPlanAttempts1", String.valueOf(attempt));

        JSONParser parser = new JSONParser();
        try
        {
            JSONObject dataset = (JSONObject) parser.parse(goal.getConvDataSetString());
            ArrayList<Object> inputList = new ArrayList<Object>();
            inputList.add(dataset);

            long execStart = System.currentTimeMillis();
            ArrayList<Object> outputList = plan.runCode(inputList);
            long execTime = System.currentTimeMillis() - execStart;
            agentResults.put("execTime1", String.valueOf(execTime));

            JSONObject convDataSet = (JSONObject) outputList.get(0);
            System.out.println("A: Plan 1 finished");
            agentResults.put("planResults1", convDataSet.toString());
            goal.setConvDataSetString(convDataSet.toString());
        } catch (Exception e)
        {
            agentResults.put("planResults1", String.valueOf(e));
        }
    }

    @OnEnd
    public void end()
    {
        System.out.println("A: Agent " + agent.getId() + " terminated");
    }
}
